package umls2emx.convertor.beans;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UmlsRelation
{
	public static UmlsRelation create(String relationIdentifier, String cuiId1, String atomId1,
			UmlsRelationAttribute umlsRelationAttribute, String cuiId2, String atomId2)
	{
		return new AutoValue_UmlsRelation(relationIdentifier, cuiId1, atomId1, umlsRelationAttribute, cuiId2, atomId2);
	}

	public abstract String getRelationIdentifier();

	public abstract String getCuiId1();

	public abstract String getAtomId1();

	public abstract UmlsRelationAttribute getUmlsRelationAttribute();

	public abstract String getCuiId2();

	public abstract String getAtomId2();

	public String toLabel()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getCuiId2()).append(' ').append(getUmlsRelationAttribute().getRelationAttribute())
				.append(' ').append(getCuiId1());
		return stringBuilder.toString();
	}
}
