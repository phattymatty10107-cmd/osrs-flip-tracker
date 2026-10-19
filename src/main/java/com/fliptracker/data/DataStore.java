package com.fliptracker.data;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Saves each account's flip data to ~/.runelite/flip-tracker/&lt;account&gt;.json */
@Singleton
public class DataStore
{
	private static final Logger log = LoggerFactory.getLogger(DataStore.class);
	private static final File DIR = new File(RuneLite.RUNELITE_DIR, "flip-tracker");

	private final Gson gson;

	@Inject
	public DataStore(Gson gson)
	{
		this.gson = gson;
	}

	public AccountData load(long accountHash)
	{
		File file = file(accountHash);
		if (!file.exists())
		{
			return new AccountData();
		}

		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
		{
			AccountData data = gson.fromJson(reader, AccountData.class);
			return data == null ? new AccountData() : data.normalize();
		}
		catch (Exception e)
		{
			log.warn("Could not read flip data from {}", file, e);
			return new AccountData();
		}
	}

	public String toJson(AccountData data)
	{
		return gson.toJson(data);
	}

	/** Write JSON produced by {@link #toJson}. Safe to call off the client thread. */
	public synchronized void write(long accountHash, String json)
	{
		try
		{
			Files.createDirectories(DIR.toPath());
			File target = file(accountHash);
			File tmp = new File(DIR, target.getName() + ".tmp");
			Files.write(tmp.toPath(), json.getBytes(StandardCharsets.UTF_8));
			Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e)
		{
			log.warn("Could not save flip data", e);
		}
	}

	private static File file(long accountHash)
	{
		return new File(DIR, accountHash + ".json");
	}
}
