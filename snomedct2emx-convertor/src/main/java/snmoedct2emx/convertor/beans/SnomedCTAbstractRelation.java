package snmoedct2emx.convertor.beans;

import snmoedct2emx.convertor.io.SnomeCTConceptRelationRepo.TypeEnum;

abstract class SnomedCTAbstractRelation
{
	private final String id;
	private final boolean active;
	private final TypeEnum type;
	private final String soureceId;
	private final String destinationId;

	public SnomedCTAbstractRelation(String id, boolean active, String sourceId, TypeEnum type, String destionationId)
	{
		this.id = id;
		this.active = active;
		this.soureceId = sourceId;
		this.type = type;
		this.destinationId = destionationId;
	}

	public String getId()
	{
		return id;
	}

	public boolean isActive()
	{
		return active;
	}

	public String getSoureceId()
	{
		return soureceId;
	}

	public String getDestinationId()
	{
		return destinationId;
	}

	public TypeEnum getType()
	{
		return type;
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
		SnomedCTAbstractRelation other = (SnomedCTAbstractRelation) obj;
		if (id == null)
		{
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}