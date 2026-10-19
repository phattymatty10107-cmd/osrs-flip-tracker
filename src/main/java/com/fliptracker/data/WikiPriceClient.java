package com.fliptracker.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client for the OSRS Wiki real-time prices API (https://prices.runescape.wiki).
 * The wiki asks for a descriptive User-Agent, so we send one.
 */
@Singleton
public class WikiPriceClient
{
	private static final HttpUrl BASE = HttpUrl.parse("https://prices.runescape.wiki/api/v1/osrs/");
	private static final String USER_AGENT = "RuneLite Flip Tracker plugin - github.com/phattymatty10107-cmd/osrs-flip-tracker";

	private static final Type MAPPING_TYPE = new TypeToken<List<Mapping>>()
	{
	}.getType();

	private final OkHttpClient http;
	private final Gson gson;

	@Inject
	public WikiPriceClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	public List<Mapping> fetchMapping() throws IOException
	{
		List<Mapping> list = get("mapping", MAPPING_TYPE);
		return list == null ? Collections.emptyList() : list;
	}

	public Map<Integer, Latest> fetchLatest() throws IOException
	{
		LatestResponse r = get("latest", LatestResponse.class);
		return r == null || r.data == null ? Collections.emptyMap() : r.data;
	}

	/** Average prices and volumes over the last hour. */
	public Map<Integer, Average> fetchHourly() throws IOException
	{
		return fetchAverage("1h");
	}

	/** Average prices and volumes over the last 24 hours. */
	public Map<Integer, Average> fetchDaily() throws IOException
	{
		return fetchAverage("24h");
	}

	private Map<Integer, Average> fetchAverage(String path) throws IOException
	{
		AverageResponse r = get(path, AverageResponse.class);
		return r == null || r.data == null ? Collections.emptyMap() : r.data;
	}

	private <T> T get(String path, Type type) throws IOException
	{
		Request request = new Request.Builder()
			.url(BASE.newBuilder().addPathSegment(path).build())
			.header("User-Agent", USER_AGENT)
			.build();

		try (Response response = http.newCall(request).execute())
		{
			ResponseBody body = response.body();
			if (!response.isSuccessful() || body == null)
			{
				throw new IOException("OSRS Wiki returned HTTP " + response.code() + " for /" + path);
			}
			return gson.fromJson(body.charStream(), type);
		}
	}

	public static class Mapping
	{
		public int id;
		public String name;
		public boolean members;
		public Integer limit;
	}

	public static class Latest
	{
		public Long high;
		public Long highTime;
		public Long low;
		public Long lowTime;
	}

	public static class Average
	{
		public Long avgHighPrice;
		public Long highPriceVolume;
		public Long avgLowPrice;
		public Long lowPriceVolume;
	}

	private static class LatestResponse
	{
		Map<Integer, Latest> data;
	}

	private static class AverageResponse
	{
		Map<Integer, Average> data;
	}
}
