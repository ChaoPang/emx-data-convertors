package org.opal2emx.convertor;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_Category.class)
public abstract class Category
{
	public static Category create(String code, String label)
	{
		return new AutoValue_Category(code, label);
	}

	public abstract String getCode();

	public abstract String getLabel();
}
