package com.fliptracker;

import com.fliptracker.data.AccountData;
import com.fliptracker.data.AccountData.LimitWindow;
import com.fliptracker.data.AccountData.SlotSnapshot;
import com.fliptracker.data.AccountData.Trade;
import com.fliptracker.data.DataStore;
import com.fliptracker.data.FlipSummary;
import com.fliptracker.data.ItemInfo;
import com.fliptracker.data.LimitView;
import com.fliptracker.data.WikiPriceClient;
import com.fliptracker.ui.FlipTrackerPanel;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Flip Tracker",
	description = "Live GE margins, best 24h flips, watchlist, flip log with profit, and buy limit timers",
	tags = {"ge", "grand exchange", "flipping", "merch", "margin", "profit"}
)
public class FlipTrackerPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(FlipTrackerPlugin.class);

	public static final long LIMIT_WINDOW_MS = TimeUnit.HOURS.toMillis(4);
	/** Ignore EMPTY slot events for a few ticks after login; the client sends bogus ones. */
	private static final int LOGIN_GRACE_TICKS = 3;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private FlipTrackerConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private ScheduledExecutorService executor;
	@Inject
	private Notifier notifier;
	@Inject
	private WikiPriceClient wiki;
	@Inject
	private DataStore store;

	private FlipTrackerPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> priceTask;
	private ScheduledFuture<?> limitTask;

	private volatile Map<Integer, WikiPriceClient.Mapping> mapping = Collections.emptyMap();
	private volatile Map<Integer, ItemInfo> prices = Collections.emptyMap();
	private volatile long pricesUpdatedAt;
	private volatile String priceError;
	private volatile FlipSummary summary = FlipSummary.EMPTY;
	private volatile List<LimitView> limitViews = Collections.emptyList();

	private final Set<Integer> watchlist = Collections.synchronizedSet(new LinkedHashSet<>());
	private final Set<Integer> marginAlerted = ConcurrentHashMap.newKeySet();
	private final Set<String> limitNotified = ConcurrentHashMap.newKeySet();

	// Client-thread state
	private long accountHash = -1;
	private AccountData data;
	private int loginGraceTicks;

	@Provides
	FlipTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlipTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		loadWatchlist();

		panel = new FlipTrackerPanel(this, itemManager);
		navButton = NavigationButton.builder()
			.tooltip("Flip Tracker")
			.icon(createIcon())
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		schedulePriceRefresh();
		limitTask = executor.scheduleWithFixedDelay(this::checkLimitResets, 30, 30, TimeUnit.SECONDS);

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				ensureAccount();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		if (priceTask != null)
		{
			priceTask.cancel(false);
		}
		if (limitTask != null)
		{
			limitTask.cancel(false);
		}
		clientToolbar.removeNavigation(navButton);
		panel.shutDown();
		panel = null;
		accountHash = -1;
		data = null;
	}

	// ---------------------------------------------------------------- getters for the panel

	public FlipTrackerConfig getConfig()
	{
		return config;
	}

	public Map<Integer, ItemInfo> getPrices()
	{
		return prices;
	}

	public long getPricesUpdatedAt()
	{
		return pricesUpdatedAt;
	}

	public String getPriceError()
	{
		return priceError;
	}

	public FlipSummary getSummary()
	{
		return summary;
	}

	public List<LimitView> getLimitViews()
	{
		return limitViews;
	}

	// ---------------------------------------------------------------- events

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loginGraceTicks = LOGIN_GRACE_TICKS;
			ensureAccount();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (loginGraceTicks > 0)
		{
			loginGraceTicks--;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!FlipTrackerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if ("refreshMinutes".equals(event.getKey()))
		{
			schedulePriceRefresh();
		}
		else if (!"watchlist".equals(event.getKey()))
		{
			updatePanel(FlipTrackerPanel::refreshPrices);
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (!ensureAccount())
		{
			return;
		}

		int slot = event.getSlot();
		if (slot < 0 || slot >= AccountData.SLOTS)
		{
			return;
		}

		GrandExchangeOffer offer = event.getOffer();
		GrandExchangeOfferState state = offer.getState();
		SlotSnapshot[] slots = data.getSlots();

		if (state == GrandExchangeOfferState.EMPTY)
		{
			boolean loggingIn = client.getGameState() != GameState.LOGGED_IN || loginGraceTicks > 0;
			if (!loggingIn && slots[slot] != null)
			{
				slots[slot] = null;
				save();
			}
			return;
		}

		boolean buy = state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY;

		SlotSnapshot current = new SlotSnapshot(offer.getItemId(), buy, offer.getPrice(),
			offer.getTotalQuantity(), offer.getQuantitySold(), offer.getSpent(), state);
		SlotSnapshot previous = slots[slot];

		// Work out how much filled since we last looked at this slot.
		int baseQty = 0;
		long baseSpent = 0;
		boolean continuing = previous != null
			&& previous.sameOffer(current)
			&& !(previous.isFinished() && !current.isFinished())
			&& current.getQuantitySold() >= previous.getQuantitySold();
		if (continuing)
		{
			baseQty = previous.getQuantitySold();
			baseSpent = previous.getSpent();
		}

		int filled = current.getQuantitySold() - baseQty;
		long gp = current.getSpent() - baseSpent;
		slots[slot] = current;

		if (filled > 0)
		{
			recordTrade(current.getItemId(), buy, filled, gp / filled);
		}
		else
		{
			save();
		}
	}

	// ---------------------------------------------------------------- trades & limits

	private void recordTrade(int itemId, boolean buy, int quantity, long price)
	{
		String name = itemName(itemId);
		long now = System.currentTimeMillis();
		data.addTrade(new Trade(itemId, name, buy, quantity, price, now));

		if (buy)
		{
			Map<Integer, LimitWindow> limits = data.getLimits();
			LimitWindow window = limits.get(itemId);
			if (window == null || now >= window.getWindowStart() + LIMIT_WINDOW_MS)
			{
				window = new LimitWindow(itemId, name, now, 0);
				limits.put(itemId, window);
			}
			window.setBought(window.getBought() + quantity);
		}

		log.debug("Recorded {} {} x{} @ {}", buy ? "buy" : "sell", name, quantity, price);
		save();
		recompute();
	}

	private String itemName(int itemId)
	{
		try
		{
			return itemManager.getItemComposition(itemId).getName();
		}
		catch (Exception e)
		{
			WikiPriceClient.Mapping m = mapping.get(itemId);
			return m != null ? m.name : "Item " + itemId;
		}
	}

	/** Load this account's data if we haven't yet. Client thread only. */
	private boolean ensureAccount()
	{
		long hash = client.getAccountHash();
		if (hash == -1)
		{
			return data != null;
		}
		if (hash != accountHash || data == null)
		{
			accountHash = hash;
			data = store.load(hash);
			recompute();
		}
		return true;
	}

	private void save()
	{
		if (data == null || accountHash == -1)
		{
			return;
		}
		long hash = accountHash;
		String json = store.toJson(data);
		executor.execute(() -> store.write(hash, json));
	}

	/** Rebuild the read-only snapshots the panel uses. Client thread only. */
	private void recompute()
	{
		if (data == null)
		{
			return;
		}

		summary = FlipSummary.compute(data.getTrades());

		long now = System.currentTimeMillis();
		List<LimitView> views = new ArrayList<>();
		Iterator<LimitWindow> it = data.getLimits().values().iterator();
		while (it.hasNext())
		{
			LimitWindow w = it.next();
			long resetAt = w.getWindowStart() + LIMIT_WINDOW_MS;
			if (now >= resetAt + TimeUnit.HOURS.toMillis(1))
			{
				it.remove(); // long expired, tidy up
				continue;
			}
			WikiPriceClient.Mapping m = mapping.get(w.getItemId());
			int limit = m != null && m.limit != null ? m.limit : 0;
			views.add(new LimitView(w.getItemId(), w.getItemName(), w.getBought(), limit, w.getWindowStart(), resetAt));
		}
		views.sort((a, b) -> Long.compare(a.getResetAt(), b.getResetAt()));
		limitViews = Collections.unmodifiableList(views);

		updatePanel(p ->
		{
			p.refreshFlips();
			p.refreshLimits();
		});
	}

	public void clearHistory()
	{
		clientThread.invokeLater(() ->
		{
			if (data == null)
			{
				return;
			}
			data.getTrades().clear();
			data.getLimits().clear();
			save();
			recompute();
		});
	}

	private void checkLimitResets()
	{
		if (!config.notifyLimitReset())
		{
			return;
		}
		long now = System.currentTimeMillis();
		for (LimitView v : limitViews)
		{
			String key = v.getItemId() + ":" + v.getWindowStart();
			if (now >= v.getResetAt() && now < v.getResetAt() + TimeUnit.MINUTES.toMillis(10) && limitNotified.add(key))
			{
				notifier.notify("Buy limit reset: " + v.getItemName());
			}
		}
	}

	// ---------------------------------------------------------------- prices

	private void schedulePriceRefresh()
	{
		if (priceTask != null)
		{
			priceTask.cancel(false);
		}
		priceTask = executor.scheduleWithFixedDelay(this::refreshPrices, 0, config.refreshMinutes(), TimeUnit.MINUTES);
	}

	public void refreshPricesNow()
	{
		executor.execute(this::refreshPrices);
	}

	private void refreshPrices()
	{
		try
		{
			if (mapping.isEmpty())
			{
				mapping = wiki.fetchMapping().stream()
					.collect(Collectors.toMap(m -> m.id, m -> m, (a, b) -> a));
			}

			Map<Integer, WikiPriceClient.Latest> latest = wiki.fetchLatest();
			Map<Integer, WikiPriceClient.Average> hourly = wiki.fetchHourly();
			Map<Integer, WikiPriceClient.Average> daily = wiki.fetchDaily();

			Map<Integer, ItemInfo> built = new HashMap<>();
			for (WikiPriceClient.Mapping m : mapping.values())
			{
				WikiPriceClient.Latest l = latest.get(m.id);
				if (l == null)
				{
					continue;
				}
				WikiPriceClient.Average h = hourly.get(m.id);
				WikiPriceClient.Average d = daily.get(m.id);

				built.put(m.id, new ItemInfo(
					m.id,
					m.name,
					m.members,
					m.limit == null ? 0 : m.limit,
					nz(l.high), nz(l.low), nz(l.highTime), nz(l.lowTime),
					h == null ? 0 : nz(h.highPriceVolume) + nz(h.lowPriceVolume),
					d == null ? 0 : nz(d.avgHighPrice),
					d == null ? 0 : nz(d.avgLowPrice),
					d == null ? 0 : nz(d.highPriceVolume),
					d == null ? 0 : nz(d.lowPriceVolume)
				));
			}

			prices = Collections.unmodifiableMap(built);
			pricesUpdatedAt = System.currentTimeMillis();
			priceError = null;
			checkMarginAlerts();
			clientThread.invokeLater(this::recompute); // limits need item buy limits from mapping
			updatePanel(FlipTrackerPanel::refreshPrices);
		}
		catch (Exception e)
		{
			log.warn("Failed to fetch prices from the OSRS Wiki", e);
			priceError = e.getMessage();
			updatePanel(FlipTrackerPanel::refreshPrices);
		}
	}

	private void checkMarginAlerts()
	{
		int threshold = config.marginAlert();
		if (threshold <= 0)
		{
			return;
		}
		for (Integer id : getWatchlist())
		{
			ItemInfo info = prices.get(id);
			if (info == null || !info.hasPrices())
			{
				continue;
			}
			if (info.margin() >= threshold)
			{
				if (marginAlerted.add(id))
				{
					notifier.notify(info.getName() + " margin is " + info.margin() + " gp");
				}
			}
			else
			{
				marginAlerted.remove(id);
			}
		}
	}

	private static long nz(Long v)
	{
		return v == null ? 0 : v;
	}

	// ---------------------------------------------------------------- watchlist

	public List<Integer> getWatchlist()
	{
		synchronized (watchlist)
		{
			return new ArrayList<>(watchlist);
		}
	}

	public boolean isWatched(int itemId)
	{
		return watchlist.contains(itemId);
	}

	public void toggleWatch(int itemId)
	{
		synchronized (watchlist)
		{
			if (!watchlist.remove(itemId))
			{
				watchlist.add(itemId);
			}
			marginAlerted.remove(itemId);
			configManager.setConfiguration(FlipTrackerConfig.GROUP, "watchlist",
				watchlist.stream().map(String::valueOf).collect(Collectors.joining(",")));
		}
		updatePanel(FlipTrackerPanel::refreshPrices);
	}

	private void loadWatchlist()
	{
		watchlist.clear();
		String raw = config.watchlist();
		if (raw == null || raw.isEmpty())
		{
			return;
		}
		for (String part : raw.split(","))
		{
			try
			{
				watchlist.add(Integer.parseInt(part.trim()));
			}
			catch (NumberFormatException ignored)
			{
			}
		}
	}

	// ---------------------------------------------------------------- helpers

	private void updatePanel(java.util.function.Consumer<FlipTrackerPanel> action)
	{
		SwingUtilities.invokeLater(() ->
		{
			FlipTrackerPanel p = panel;
			if (p != null)
			{
				action.accept(p);
			}
		});
	}

	private static BufferedImage createIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0xE0B030));
		g.fillOval(1, 1, 14, 14);
		g.setColor(new Color(0x7A5A10));
		g.setStroke(new BasicStroke(1.2f));
		g.drawOval(1, 1, 14, 14);
		// two arrows: buy low, sell high
		g.setColor(new Color(0x2E7D32));
		g.fillPolygon(new int[]{5, 8, 11}, new int[]{8, 4, 8}, 3);
		g.setColor(new Color(0xB71C1C));
		g.fillPolygon(new int[]{5, 8, 11}, new int[]{9, 13, 9}, 3);
		g.dispose();
		return img;
	}
}
