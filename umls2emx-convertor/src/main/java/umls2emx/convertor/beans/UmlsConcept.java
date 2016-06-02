package umls2emx.convertor.beans;

import java.util.List;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UmlsConcept
{
	public static UmlsConcept create(String cuiId, String preferredName, List<UmlsAtom> conceptAtoms,
			List<Synonym> synonmys)
	{
		return new AutoValue_UmlsConcept(cuiId, preferredName, conceptAtoms, synonmys);
	}

	public abstract String getCuiId();

	public abstract String getPreferredName();

	public abstract List<UmlsAtom> getConceptAtoms();

	public abstract List<Synonym> getSynonyms();
}
