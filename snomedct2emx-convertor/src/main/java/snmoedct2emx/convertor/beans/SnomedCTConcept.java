package snmoedct2emx.convertor.beans;

public class SnomedCTConcept
{
	private final String id;
	private final boolean active;

	public SnomedCTConcept(String id, boolean active)
	{
		this.id = id;
		this.active = active;
	}

	public String getId()
	{
		return id;
	}

	public boolean isActive()
	{
		return active;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SnomedCTConcept other = (SnomedCTConcept) obj;
		if (id == null)
		{
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}
