package snmoedct2emx.convertor.beans;

public class SnomedCTDescription
{
	private final String id;
	private final String conceptId;
	private final boolean active;
	private final TypeEnum type;
	private final String term;

	private enum TypeEnum
	{
		LABEL("900000000000003001"), SYNONYM("900000000000013009"), DEFINITION("900000000000550004");

		private String label;

		TypeEnum(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	};

	public SnomedCTDescription(String id, String conceptId, boolean active, String typeId, String term)
	{
		this.id = id;
		this.conceptId = conceptId;
		this.active = active;
		this.type = convertToEnumType(typeId);
		this.term = term;
	}

	public String getId()
	{
		return id;
	}

	public String getConceptId()
	{
		return conceptId;
	}

	public boolean isActive()
	{
		return active;
	}

	public TypeEnum getType()
	{
		return type;
	}

	public String getTerm()
	{
		return term;
	}

	public boolean isLabel()
	{
		return type.equals(TypeEnum.LABEL);
	}

	public boolean isDefinition()
	{
		return type.equals(TypeEnum.DEFINITION);
	}

	private TypeEnum convertToEnumType(String typeId)
	{
		for (TypeEnum type : TypeEnum.values())
		{
			if (type.toString().equals(typeId)) return type;
		}
		throw new RuntimeException("No matched Type has been found for : " + typeId);
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
		SnomedCTDescription other = (SnomedCTDescription) obj;
		if (id == null)
		{
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}