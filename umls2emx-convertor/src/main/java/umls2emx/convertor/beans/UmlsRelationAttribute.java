package umls2emx.convertor.beans;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.umls.beans.AutoValue_UmlsRelationAttribute;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UmlsRelationAttribute
{
	public static UmlsRelationAttribute create(String relationAttribute)
	{
		return new AutoValue_UmlsRelationAttribute(relationAttribute, createDescription(relationAttribute));
	}

	public static UmlsRelationAttribute create(String relationAttribute, String relationDescription)
	{
		return new AutoValue_UmlsRelationAttribute(relationAttribute, relationDescription);
	}

	public abstract String getRelationAttribute();

	@Nullable
	public abstract String getRelationDescription();

	private static String createDescription(String relationLabel)
	{
		return StringUtils.isBlank(relationLabel) ? StringUtils.EMPTY : relationLabel.replaceAll("_", " ");
	}
}
