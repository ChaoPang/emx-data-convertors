package org.opal2emx.convertor;

import javax.annotation.Nullable;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OpalVariable.class)
public abstract class OpalVariable
{
	public static OpalVariable create(String variableName, String label, String dataType)
	{
		return new AutoValue_OpalVariable(variableName, label, dataType, null, null, null, null);
	}

	public static OpalVariable create(String variableName, String label, String dataType, Double min, Double max,
			Double mean, Double stddev)
	{
		return new AutoValue_OpalVariable(variableName, label, dataType, min, max, mean, stddev);
	}

	public abstract String getVariableName();

	public abstract String getLabel();

	public abstract String getDataType();

	@Nullable
	public abstract Double getMin();

	@Nullable
	public abstract Double getMax();

	@Nullable
	public abstract Double getMean();

	@Nullable
	public abstract Double getStddev();
}