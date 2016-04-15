package umls2emx.convertor.beans;

import java.util.List;

import org.molgenis.umls.beans.AutoValue_UmlsConcept;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UmlsConcept
{
	public static UmlsConcept create(String cuiId, String preferredName, List<UmlsAtom> conceptAtoms)
	{
		return new AutoValue_UmlsConcept(cuiId, preferredName, conceptAtoms);
	}

	public abstract String getCuiId();

	public abstract String getPreferredName();

	public abstract List<UmlsAtom> getConceptAtoms();
}
