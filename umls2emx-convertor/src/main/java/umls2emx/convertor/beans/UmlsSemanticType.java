package umls2emx.convertor.beans;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UmlsSemanticType
{
	public abstract String getIdentifier();

	public abstract String getSemanticTypeName();

	public abstract String getGroupName();

	public static UmlsSemanticType create(String identifier, String semanticTypeName, String groupName)
	{
		return new AutoValue_UmlsSemanticType(identifier, semanticTypeName, groupName);
	}
}
