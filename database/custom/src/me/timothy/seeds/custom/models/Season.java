package me.timothy.seeds.custom.models;

import me.timothy.seeds.shared.ObjectWithID;

/**
 * Describes a planting season, such as "Early Fall" or "Late Summer".
 * 
 * @author Timothy
 */
public class Season implements ObjectWithID {
	/** The unique identifier for this season */
	public int id;
	
	/** The name of the season, with spaces and title-case. Maximum length 63 characters */
	public String name;

	public Season(int id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	
	@Override
	public int id() {
		return id;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Season other = (Season) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Season [id=" + id + ", name=" + name + "]";
	}
}
