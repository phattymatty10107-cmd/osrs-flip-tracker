# Flip Tracker: a RuneLite plugin for Grand Exchange flipping

A side panel in RuneLite with five tabs:

| Tab | What it does |
|---|---|
| **Live** | Best flips right now from live OSRS Wiki prices: buy/sell price, margin after the 2% GE tax, ROI, hourly volume, buy limit. You can sort by profit per limit, margin, ROI or volume, and search any item. |
| **24h** | The best items to flip over a full day. It ranks by estimated 24h profit = 24h average margin (after tax) × how many you can flip in a day, which is the smaller of (buy limit × 6 four-hour windows) and daily volume. Hover a row for the breakdown and how much cash one limit needs. |
| **Watch** | Your watchlist. Add items by name, or click any item in Live/24h. You can get an alert when a watched item's margin reaches a set amount. |
| **Flips** | Records your GE buys and sells automatically. Shows total profit, today's profit, flip count, tax paid, items you're still holding (with unrealised profit), and your completed flips. |
| **Limits** | Countdown to each item's 4-hour buy limit reset, with an optional notification when it resets. |

Click any item row for options (watchlist, open the wiki price chart). Filters such as min volume, price range and members items are in the plugin's settings (the cog in the plugin list).

## Run it

You need **Java 11 or newer** (JDK 17 recommended) and **IntelliJ IDEA Community** (free).

1. Unzip this folder.
2. In IntelliJ, choose **File → Open** and pick the `osrs-flip-tracker` folder. Let Gradle finish importing.
3. Open `src/test/java/com/fliptracker/FlipTrackerPluginTest.java` and click the green ▶ next to `main`.
   - Or, from the Gradle panel, run **Tasks → application → runClient**.
4. RuneLite starts with Flip Tracker loaded. Click the gold coin icon in the sidebar.

To use it in your normal RuneLite without IntelliJ, submit it to the **Plugin Hub**: push this folder to a public GitHub repo, then open a PR on https://github.com/runelite/plugin-hub following their README. `runelite-plugin.properties` is already filled in.

## How the tracking works

- It listens for GE offer updates and records each fill (including partial fills). It remembers each slot's last state, so offers that fill while you're logged out are counted the next time you log in, and nothing is counted twice.
- Profit matches each sell to your earlier buys of the same item, oldest first (FIFO). The 2% tax is applied per item: rounded down, capped at 5m, nothing under 50gp, and bonds are exempt.
- Sells with no recorded buy (items you had before installing the plugin) aren't counted as profit.
- Data is saved per account in `~/.runelite/flip-tracker/<account>.json`.

## Notes

- Prices come from the OSRS Wiki real-time prices API (prices.runescape.wiki) and refresh every 5 minutes by default.
- The 24h estimates assume you can get your share of the daily volume at average prices. Treat them as a ranking guide, not a promise. Very thin or manipulated items can look better than they are, so raise **Min daily volume** if you see junk.
- Flip Tracker only reads GE state and shows information. It doesn't click or automate anything, so it stays within Jagex's third-party client rules.
