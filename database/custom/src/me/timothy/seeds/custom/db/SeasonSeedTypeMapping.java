package me.timothy.seeds.custom.db;

import me.timothy.seeds.custom.models.SeasonSeedType;
import me.timothy.seeds.shared.db.Mapping;

public interface SeasonSeedTypeMapping extends Mapping<SeasonSeedType> {
	/**
	 * Fetch all of the plant/harvest combinations given the seed type id
	 * 
	 * @param seedTypeID the ID of the seed type
	 * @return the corresponding season seed types
	 */
	public SeasonSeedType[] fetchBySeedType(int seedTypeID);
	
	/**
	 * Fetch all of the seed/harvest combinations given the plant season id
	 * 
	 * @param plantSeasonID the season that you want to plant in
	 * @return the corresponding season seed types
	 */
	public SeasonSeedType[] fetchByPlantSeason(int plantSeasonID);
	
	/**
	 * Fetch all of the seed/plant combinations given the harvest season id
	 * 
	 * @param harvestSeasonID the harvest season
	 * @return the corresponding season seed types
	 */
	public SeasonSeedType[] fetchByHarvestSeason(int harvestSeasonID);
}
