package umls2emx.convertor.beans;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.umls.beans.AutoValue_UmlsAtom;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UmlsAtom
{
	public static UmlsAtom create(String atomId, String atomName, String atomSource)
	{
		return new AutoValue_UmlsAtom(atomId, atomName, atomSource, StringUtils.EMPTY);
	}

	public static UmlsAtom create(String atomId, String atomName, String atomSource, String hierachy)
	{
		return new AutoValue_UmlsAtom(atomId, atomName, atomSource, hierachy);
	}

	public abstract String getAtomId();

	@Nullable
	public abstract String getAtomName();

	public abstract String getAtomSource();

	@Nullable
	public abstract String getHierachy();
}
