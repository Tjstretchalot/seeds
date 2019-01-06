package me.timothy.seeds.custom.models;

/**
 * Describes a map between seasons and the seed types that can be planted during those
 * seasons, and finally when they can be harvested when they are planted in that season.
 * 
 * @author Timothy
 */
public class SeasonSeedType {
	/** The id of the seed type which can be planted during the plant season */
	public int seedTypeID;
	/** The season in which the seed can be planted */
	public int plantSeasonID;
	/** The season in which the seed can be harvested */
	public int harvestSeasonID;
	
	
	public SeasonSeedType(int seedTypeID, int plantSeasonID, int harvestSeasonID) {
		super();
		this.seedTypeID = seedTypeID;
		this.plantSeasonID = plantSeasonID;
		this.harvestSeasonID = harvestSeasonID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + harvestSeasonID;
		result = prime * result + plantSeasonID;
		result = prime * result + seedTypeID;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SeasonSeedType other = (SeasonSeedType) obj;
		if (harvestSeasonID != other.harvestSeasonID)
			return false;
		if (plantSeasonID != other.plantSeasonID)
			return false;
		if (seedTypeID != other.seedTypeID)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "SeasonSeedType [seedTypeID=" + seedTypeID + ", plantSeasonID=" + plantSeasonID + ", harvestSeasonID="
				+ harvestSeasonID + "]";
	}
}
