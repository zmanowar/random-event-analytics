/*
 * Copyright (c) 2018
 * 	TheStonedTurtle <https://github.com/TheStonedTurtle>, zmanowar <https://github.com/zmanowar>
 * All rights reserved.
 *
 * Modified source from https://github.com/TheStonedTurtle/Loot-Logger/
 */
package com.randomEventAnalytics.localstorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.Getter;
import static net.runelite.client.RuneLite.RUNELITE_DIR;
import net.runelite.http.api.RuneLiteAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Either break the random events into seperate files
 * or implement/import a DB system.
 **/
public class RandomEventAnalyticsLocalStorage
{
	private static final String FILE_EXTENSION = ".log";
	private static final File RANDOM_EVENT_RECORD_DIR = new File(RUNELITE_DIR, "random-event-analytics");
	private static final String RANDOM_EVENTS_FILE = "random-events";
	private static final Logger log = LoggerFactory.getLogger(RandomEventAnalyticsLocalStorage.class);
	private File playerFolder;
	@Getter
	private int numberOfLoggedEvents = 0;
	@Getter
	private String username;

	@Inject
	public RandomEventAnalyticsLocalStorage()
	{
		RANDOM_EVENT_RECORD_DIR.mkdir();
	}


	public boolean setPlayerUsername(final String username)
	{
		if (username.equalsIgnoreCase(this.username))
		{
			return false;
		}

		playerFolder = new File(RANDOM_EVENT_RECORD_DIR, username);
		playerFolder.mkdir();
		this.username = username;
		return true;
	}

	private File getFile(String fileName)
	{
		return new File(playerFolder, fileName + FILE_EXTENSION);
	}

	public synchronized ArrayList<RandomEventRecord> loadRandomEventRecords()
	{
		final File file = getFile(RANDOM_EVENTS_FILE);
		final ArrayList<RandomEventRecord> data = new ArrayList<>();

		try (final BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				// Skips the empty line at end of file
				if (line.length() > 0)
				{
					final RandomEventRecord r = RuneLiteAPI.GSON.fromJson(line, RandomEventRecord.class);
					data.add(r);
				}
			}

		}
		catch (FileNotFoundException e)
		{
			log.debug("File not found: {}", file.getName());
		}
		catch (IOException e)
		{
			log.warn("IOException for file {}: {}", file.getName(), e.getMessage());
		}

		numberOfLoggedEvents = data.size();
		return data;
	}

	public synchronized RandomEventRecord getMostRecentRandom()
	{
		final ArrayList<RandomEventRecord> data = loadRandomEventRecords();
		if (data.size() > 0)
		{
			return data.get(data.size() - 1);
		}

		return null;
	}

	public synchronized boolean renameUsernameFolderToAccountHash(final String username, final long hash)
	{
		final File usernameDir = new File(RANDOM_EVENT_RECORD_DIR, username);
		if (!usernameDir.exists())
		{
			return true;
		}

		final File hashDir = new File(RANDOM_EVENT_RECORD_DIR, String.valueOf(hash));
		if (hashDir.exists())
		{
			log.warn("Can't rename username folder to account hash as the folder for this account hash already exists" + "." + " This was most likely caused by running RL through the Jagex launcher before the migration code" + " was" + " added");
			log.warn("Username: {} | AccountHash: {}", username, hash);
			return false;
		}

		return usernameDir.renameTo(hashDir);
	}

	public synchronized boolean addRandomEventRecord(RandomEventRecord rec)
	{
		final File randomEventsFile = getFile(RANDOM_EVENTS_FILE);

		// Convert entry to JSON
		final String dataAsString = RuneLiteAPI.GSON.toJson(rec);

		// Open File in append mode and write new data
		try
		{
			final BufferedWriter file = new BufferedWriter(new FileWriter(String.valueOf(randomEventsFile), true));
			file.append(dataAsString);
			file.newLine();
			file.close();
			numberOfLoggedEvents += 1;
			return true;
		}
		catch (IOException ioe)
		{
			log.warn("Error writing loot data to file {}: {}", randomEventsFile.getName(), ioe.getMessage());
			return false;
		}
	}

}
