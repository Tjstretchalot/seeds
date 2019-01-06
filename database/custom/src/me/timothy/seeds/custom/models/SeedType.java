package me.timothy.seeds.custom.models;

/**
 * Describes a type of seed. Seeds can be planted during various seasons and can be harvested 
 * based on which season they were planted in.
 * 
 * @author Timothy
 */
public class SeedType {
	/** A unique identifier for this seed */
	public int id;

	public SeedType(int id) {
		super();
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		SeedType other = (SeedType) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SeedType [id=" + id + "]";
	}
	
	
}
