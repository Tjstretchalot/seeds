package me.timothy.seeds.custom.db;

import java.util.Iterator;

import me.timothy.seeds.custom.models.Season;
import me.timothy.seeds.shared.db.Mapping;

/**
 * Describes the necessary functions for something which stores and retrieves seasons.
 *  
 * @author Timothy
 */
public interface SeasonMapping extends Mapping<Season> {
	/**
	 * Fetch the season associated with the given ID.
	 * @param id the id of the season
	 * @return the corresponding season
	 */
	public Season fetchByID(int id);
	
	/**
	 * Retrieves something capable of iterating over of the seasons.
	 * 
	 * @return an iterator for the seasons.
	 */
	public Iterator<Season> fetchIter();
}
