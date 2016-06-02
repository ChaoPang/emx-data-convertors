package umls2emx.convertor.beans;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Synonym
{
	public abstract String getId();

	public abstract String getName();

	public static Synonym create(String id, String name)
	{
		return new AutoValue_Synonym(id, name);
	}
}
