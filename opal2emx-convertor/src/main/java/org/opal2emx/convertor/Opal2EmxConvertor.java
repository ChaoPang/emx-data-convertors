package org.opal2emx.convertor;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.molgenis.data.Entity;
import org.molgenis.data.MolgenisInvalidFormatException;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.Writable;
import org.molgenis.data.WritableFactory;
import org.molgenis.data.excel.ExcelRepositoryCollection;
import org.molgenis.data.excel.ExcelWriter;
import org.molgenis.data.processor.TrimProcessor;
import org.molgenis.data.support.MapEntity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Opal2EmxConvertor
{
	private final int BOOTSTRAP_TIME = 10000;
	private final Random random = new Random();
	private final DecimalFormat decimalFormat = new DecimalFormat("##.##", new DecimalFormatSymbols(Locale.ENGLISH));
	public Map<String, List<Category>> variableCategoryLinks = new LinkedHashMap<String, List<Category>>();
	public Map<String, OpalVariable> variableInfo = new LinkedHashMap<String, OpalVariable>();
	public final static String ILLEGAL_CHAR_ENTITY = "[^a-zA-Z0-9_#]";

	public String studyName = null;

	public Opal2EmxConvertor(String studyName, String filePath) throws IOException, MolgenisInvalidFormatException
	{
		this.studyName = studyName;
		start(filePath);
	}

	public void start(String fileString) throws IOException, MolgenisInvalidFormatException
	{
		File file = new File(fileString);
		if (file.exists())
		{

			RepositoryCollection repositoryCollection = new ExcelRepositoryCollection(file, new TrimProcessor());

			// Handle category sheet first
			collectCategoryInfo(repositoryCollection);
			// Handle variable sheet second
			collectVariableInfo(repositoryCollection);

			ExcelWriter writer = null;

			try
			{
				writer = new ExcelWriter(new File(file.getAbsolutePath() + "_emx.xls"));
				writeToEMXFormat(writer);
			}
			finally
			{
				writer.close();
			}
		}
	}

	public void collectVariableInfo(RepositoryCollection repositoryCollection) throws IOException
	{
		Repository repo = repositoryCollection.getRepository("Variables");
		for (Entity entity : repo)
		{
			String variableName = removeIllegalCharsFromEntityName(entity.getString("name"));
			String label = entity.getString("label:en");
			String opalDataType = entity.getString("valueType");

			Double minimum = entity.getDouble("minimum");
			Double maximum = entity.getDouble("maximum");
			Double mean = entity.getDouble("mean");
			Double stddev = entity.getDouble("stddev");
			variableInfo.put(variableName,
					OpalVariable.create(variableName, label, opalDataType, minimum, maximum, mean, stddev));
		}
	}

	public void collectCategoryInfo(RepositoryCollection repositoryCollection) throws IOException
	{
		Repository repo = repositoryCollection.getRepository("Categories");
		for (Entity entity : repo)
		{
			String variableName = removeIllegalCharsFromEntityName(entity.getString("variable"));
			String code = entity.getString("name");
			String label = entity.getString("label:en");

			List<Category> categoriesPerVariable = null;
			if (variableCategoryLinks.containsKey(variableName))
			{
				categoriesPerVariable = variableCategoryLinks.get(variableName);
			}
			else
			{
				categoriesPerVariable = new ArrayList<Category>();
			}
			categoriesPerVariable.add(Category.create(code, label));
			variableCategoryLinks.put(variableName, categoriesPerVariable);
		}

		// For variables which only have one category, they are not categorical.
		for (String variableName : Sets.newHashSet(variableCategoryLinks.keySet()))
		{
			List<Category> opalCategories = variableCategoryLinks.get(variableName);
			if (opalCategories.size() == 1)
			{
				variableCategoryLinks.remove(variableName);
			}
		}
	}

	public void writeToEMXFormat(WritableFactory writer) throws IOException
	{
		// Create sheet for refEntities
		for (Entry<String, List<Category>> entrySet : variableCategoryLinks.entrySet())
		{
			Writable categoryRefEntity = null;
			try
			{
				String variableName = entrySet.getKey();

				if (variableInfo.containsKey(variableName))
				{
					categoryRefEntity = writer.createWritable(createRefEntityName(variableName),
							Arrays.asList("code", "label"));
					for (Category category : entrySet.getValue())
					{
						MapEntity categoryEntity = new MapEntity();
						categoryEntity.set("code", category.getCode());
						categoryEntity.set("label", category.getLabel());
						categoryRefEntity.add(categoryEntity);
					}
				}
			}
			finally
			{
				if (categoryRefEntity != null) categoryRefEntity.close();
			}
		}

		// Create sheet for packages
		Writable packagesEntity = null;
		try
		{
			packagesEntity = writer.createWritable("packages", Arrays.asList("name", "description", "parent"));
			packagesEntity.add(new MapEntity(ImmutableMap.of("name", createPackageName(studyName))));
		}
		finally
		{
			if (packagesEntity != null) packagesEntity.close();
		}

		// Create sheet for entities
		Writable entitySheet = null;

		try
		{
			entitySheet = writer.createWritable("entities",
					Arrays.asList("name", "package", "description", "abstract", "backend"));

			String packageName = createPackageName(studyName);
			entitySheet.add(addEntityRecord(studyName, packageName));

			for (String variableName : variableCategoryLinks.keySet())
			{
				if (variableInfo.containsKey(variableName))
				{
					entitySheet.add(addEntityRecord(createRefEntityName(variableName), packageName));
				}
			}

		}
		finally
		{
			if (entitySheet != null) entitySheet.close();
		}

		// Create sheet for attributes
		Writable attributesSheet = null;
		try
		{
			attributesSheet = writer.createWritable("attributes", Arrays.asList("name", "entity", "dataType", "label",
					"refEntity", "idAttribute", "nillable", "enumOptions", "rangeMin", "rangeMax", "lookupAttribute",
					"labelAttribute", "readOnly", "aggregateable", "visible", "unique", "partOfAttribute",
					"expression", "validationExpression"));

			attributesSheet.add(createIdAttribute());

			for (OpalVariable opalVariable : variableInfo.values())
			{
				if (variableCategoryLinks.containsKey(opalVariable.getVariableName()))
				{
					attributesSheet.add(createCategoryCodeAttribute(opalVariable));
					attributesSheet.add(createCategoryLabelAttribute(opalVariable));
				}
			}

			for (OpalVariable opalVariable : variableInfo.values())
			{
				attributesSheet.add(addAttributeRecord(opalVariable));
			}
		}
		finally
		{
			if (attributesSheet != null) attributesSheet.close();
		}

		// Create sheet for simulated dataset
		if (variableInfo.size() < 255)
		{
			Writable simulatedDataSet = null;
			try
			{
				simulatedDataSet = writer.createWritable(studyName, Lists.newArrayList(variableInfo.keySet()));
				for (int i = 1; i <= BOOTSTRAP_TIME; i++)
				{
					simulatedDataSet.add(createSimulatedRecord(i));
				}
			}
			finally
			{
				if (simulatedDataSet != null) simulatedDataSet.close();
			}
		}
	}

	private String createPackageName(String entityName)
	{
		return entityName + "_pkg";
	}

	private MapEntity createSimulatedRecord(int index)
	{
		MapEntity mapEntity = new MapEntity();
		mapEntity.set("id", index);

		for (OpalVariable opalVariable : variableInfo.values())
		{
			Object value = null;

			String variableName = opalVariable.getVariableName();
			String opalDataType = opalVariable.getDataType();
			Double randomDouble = getRandomDouble(opalVariable);
			switch (convertDataType(variableName, opalDataType))
			{
				case "categorical":
					if (variableCategoryLinks.containsKey(variableName))
					{
						value = getRandomCategory(variableName);
					}
					break;
				case "int":
					value = randomDouble != null ? randomDouble.intValue() : null;
					break;
				case "decimal":
					value = randomDouble != null ? decimalFormat.format(randomDouble.doubleValue()) : null;
					break;
				case "string":
				default:
					value = null;
			}

			mapEntity.set(variableName, value);
		}

		return mapEntity;
	}

	public String getRandomCategory(String variableName)
	{
		int size = variableCategoryLinks.get(variableName).size();
		int randomIndex = random.nextInt(size);
		Category randomCategory = variableCategoryLinks.get(variableName).get(randomIndex);
		return randomCategory.getCode();
	}

	public Double getRandomDouble(OpalVariable uniqueVariable)
	{
		if (uniqueVariable.getMin() != null && uniqueVariable.getMax() != null)
		{
			if (uniqueVariable.getMean() != null && uniqueVariable.getStddev() != null)
			{
				NormalDistribution normalDistribution = new NormalDistribution(uniqueVariable.getMean(),
						uniqueVariable.getStddev());
				return normalDistribution.sample();
			}
			else
			{
				Double min = Math.floor(uniqueVariable.getMin());
				Double max = Math.ceil(uniqueVariable.getMax());
				return min + random.nextInt(max.intValue() - min.intValue());
			}
		}
		return null;
	}

	MapEntity addEntityRecord(String entityName, String packageName)
	{
		MapEntity mapEntity = new MapEntity();
		mapEntity.set("name", entityName);
		mapEntity.set("package", packageName);
		mapEntity.set("backend", "ElasticSearch");
		return mapEntity;
	}

	MapEntity addAttributeRecord(OpalVariable variable)
	{
		String variableName = variable.getVariableName();
		String opalDataType = variable.getDataType();
		String dataType = convertDataType(variableName, opalDataType);
		String label = variable.getLabel();

		MapEntity mapEntity = new MapEntity();
		mapEntity.set("name", variableName);
		mapEntity.set("entity", studyName);
		mapEntity.set("dataType", dataType);
		mapEntity.set("label", label);
		mapEntity.set("refEntity",
				variableCategoryLinks.containsKey(variableName) ? createRefEntityName(variableName) : null);
		mapEntity.set("idAttribute", false);
		mapEntity.set("nillable", true);

		return mapEntity;
	}

	String convertDataType(String variableName, String dataType)
	{
		if (variableCategoryLinks.containsKey(variableName))
		{
			dataType = variableCategoryLinks.get(variableName).size() == 1 ? "int" : "categorical";
		}
		else if (dataType != null && dataType.equals("integer")) dataType = "int";
		else if (dataType != null && dataType.equals("decimal")) dataType = "decimal";
		else dataType = "string";
		return dataType;
	}

	Entity createCategoryLabelAttribute(OpalVariable variable)
	{
		MapEntity categoryLabelAttribute = new MapEntity();
		categoryLabelAttribute.set("name", "label");
		categoryLabelAttribute.set("entity", createRefEntityName(variable.getVariableName()));
		categoryLabelAttribute.set("dataType", "string");
		categoryLabelAttribute.set("label", "label");
		categoryLabelAttribute.set("idAttribute", false);
		categoryLabelAttribute.set("lookupAttribute", true);
		categoryLabelAttribute.set("labelAttribute", true);
		categoryLabelAttribute.set("nillable", false);
		return categoryLabelAttribute;
	}

	MapEntity createCategoryCodeAttribute(OpalVariable variable)
	{
		MapEntity categoryCodeAttribute = new MapEntity();
		categoryCodeAttribute.set("name", "code");
		categoryCodeAttribute.set("entity", createRefEntityName(variable.getVariableName()));
		categoryCodeAttribute.set("label", "code");
		categoryCodeAttribute.set("dataType", "int");
		categoryCodeAttribute.set("idAttribute", true);
		categoryCodeAttribute.set("lookupAttribute", true);
		categoryCodeAttribute.set("nillable", false);
		return categoryCodeAttribute;
	}

	Entity createIdAttribute()
	{
		MapEntity entity = new MapEntity();
		entity.set("name", "id");
		entity.set("entity", studyName);
		entity.set("dataType", "string");
		entity.set("idAttribute", "AUTO");
		entity.set("nillable", false);
		entity.set("lookupAttribute", true);
		entity.set("labelAttribute", true);
		return entity;
	}

	String createRefEntityName(String variableName)
	{
		return variableName + "_Ref";
	}

	String removeIllegalCharsFromEntityName(String entityName)
	{
		return entityName.trim().replaceAll(ILLEGAL_CHAR_ENTITY, StringUtils.EMPTY);
	}

	public static void main(String[] args) throws IOException, MolgenisInvalidFormatException
	{
		new Opal2EmxConvertor(args[0], args[1]);
	}
}