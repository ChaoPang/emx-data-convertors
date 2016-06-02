package umls2emx.convertor.beans;

import java.util.List;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UmlsHierachicalRelation
{
	public static UmlsHierachicalRelation create(String cuiId, List<UmlsAtom> atoms)
	{
		return new AutoValue_UmlsHierachicalRelation(cuiId, atoms);
	}

	public abstract String getCuiId();

	public abstract List<UmlsAtom> getAtoms();
}
