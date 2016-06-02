package umls2emx.convertor.beans;

import java.util.List;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UmlsConceptSemanticType
{
	public abstract String getIdentifier();

	public abstract String getUmlsConceptId();

	public abstract List<String> getSemanticTypeIds();

	public static UmlsConceptSemanticType create(String identifier, String umlsConceptId, List<String> semanticTypeIds)
	{
		return new AutoValue_UmlsConceptSemanticType(identifier, umlsConceptId, semanticTypeIds);
	}
}
