package org.finnish.data.convertor;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.Entity;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.meta.PackageMetaData;
import org.molgenis.data.support.MapEntity;

import com.google.common.collect.ImmutableMap;

public class FinnishMetaDataToEmxMetaData
{
	private static final Character CSV_DELIMITOR = ';';
	private static final String ATTRIBUTE_NAME_COLUMN = "LABEL";
	private static final String ATTRIBUTE_LABEL_COLUMN = "DISPLAY NAME";
	private static final String ATTRIBUTE_DESCRIPTION_COLUMN = "DESCRIPTION";
	private static final String ATTRIBUTE_DATATYPE_COLUMN = "DATATYPE";
	private static final String ATTRIBUTE_ENUMS_COLUMN = "ENUMS";
	private static final String CATEGORY_SEPARATOR = "\\)\\(";
	private static final String CATEGORY_CODE = "code";
	private static final String CATEGORY_LABEL = "label";
	private static final Pattern CATEGORY_REGEX = Pattern.compile("(\\d*)\\s\\[(.*)\\]");

	private final List<String> listOfFileNames;

	public FinnishMetaDataToEmxMetaData(List<String> listOfFileNames)
	{
		this.listOfFileNames = listOfFileNames;
	}

	public void convert(String packageName, String entityName, File inputFile, File outputFolder) throws IOException
	{
		CsvRepository csvRepository = new CsvRepository(inputFile, emptyList(), CSV_DELIMITOR);

		CsvWriter packageCsvWriter = createCsvWriter(outputFolder, PackageMetaData.ENTITY_NAME,
				Arrays.asList("name", "description", "parent"));

		packageCsvWriter.add(new MapEntity(ImmutableMap.of("name", packageName)));

		CsvWriter attributeCsvWriter = createCsvWriter(outputFolder, AttributeMetaDataMetaData.ENTITY_NAME,
				Arrays.asList("name", "entity", "dataType", "label", "refEntity", "idAttribute", "nillable",
						"enumOptions", "rangeMin", "rangeMax", "lookupAttribute", "labelAttribute", "readOnly",
						"aggregateable", "visible", "unique", "partOfAttribute", "expression", "validationExpression"));

		CsvWriter entityCsvSheetWriter = createCsvWriter(outputFolder, EntityMetaDataMetaData.ENTITY_NAME,
				Arrays.asList("name", "package", "description", "backend"));

		entityCsvSheetWriter.add(createEntityEntry(packageName, entityName));

		String entityFullName = createEntityName(packageName, entityName);

		attributeCsvWriter.add(createIdAttribute(entityFullName));

		Iterator<Entity> iterator = csvRepository.iterator();
		while (iterator.hasNext())
		{
			Entity next = iterator.next();

			String attributeName = validateName(next.getString(ATTRIBUTE_NAME_COLUMN));
			String attributeLabel = next.getString(ATTRIBUTE_LABEL_COLUMN);
			String attributeDescription = next.getString(ATTRIBUTE_DESCRIPTION_COLUMN);
			String attributeDataType = mapFinnishDataTypeToEmxDataType(next);
			String attributeEnums = next.getString(ATTRIBUTE_ENUMS_COLUMN);

			MapEntity attributeEntity = new MapEntity();
			attributeEntity.set(AttributeMetaDataMetaData.NAME, attributeName);
			attributeEntity.set(AttributeMetaDataMetaData.LABEL, attributeLabel);
			attributeEntity.set(AttributeMetaDataMetaData.DESCRIPTION, attributeDescription);
			attributeEntity.set(AttributeMetaDataMetaData.DATA_TYPE, attributeDataType);
			attributeEntity.set("entity", entityFullName);
			attributeEntity.set(AttributeMetaDataMetaData.ID_ATTRIBUTE, false);
			attributeEntity.set(AttributeMetaDataMetaData.NILLABLE, true);
			attributeEntity.set(AttributeMetaDataMetaData.LOOKUP_ATTRIBUTE, false);
			attributeEntity.set(AttributeMetaDataMetaData.LABEL_ATTRIBUTE, false);

			if (attributeDataType.equals(MolgenisFieldTypes.CATEGORICAL.toString()))
			{
				String refEntityName = createRefEntityName(entityName, attributeName);
				String refEntityFullName = createEntityName(packageName, refEntityName);
				attributeEntity.set(AttributeMetaDataMetaData.REF_ENTITY, refEntityFullName);
				CategoryRefEntityMetaData categoryRefEntityMetaData = new CategoryRefEntityMetaData(refEntityFullName);
				CsvWriter refEntityExcelSheetWriter = createCsvWriter(outputFolder, categoryRefEntityMetaData.getName(),
						categoryRefEntityMetaData.getAttributeNames());
				refEntityExcelSheetWriter.add(createRefEntities(attributeEnums));
				refEntityExcelSheetWriter.close();

				entityCsvSheetWriter.add(createEntityEntry(packageName, refEntityName));
				attributeCsvWriter.add(categoryRefEntityMetaData.getAttributeEntityForCode());
				attributeCsvWriter.add(categoryRefEntityMetaData.getAttributeEntityForLabel());
			}

			attributeCsvWriter.add(attributeEntity);
		}
		csvRepository.close();
		attributeCsvWriter.close();
		entityCsvSheetWriter.close();
		packageCsvWriter.close();
	}

	private String validateName(String string)
	{
		boolean isDigit = true;
		while (isDigit && string.length() > 0)
		{
			char c = string.charAt(0);
			isDigit = (c >= '0' && c <= '9');

			if (isDigit) string = string.substring(1);
		}

		return string;
	}

	private CsvWriter createCsvWriter(File outputFolder, String entityName, List<String> attributeNames)
			throws IOException
	{
		String outputFileName = outputFolder.getAbsolutePath() + File.separator + entityName + ".csv";
		File outputFile = new File(outputFileName);
		CsvWriter csvWriter = new CsvWriter(outputFile);
		csvWriter.writeAttributeNames(attributeNames);
		listOfFileNames.add(outputFileName);
		return csvWriter;
	}

	List<Entity> createRefEntities(String enumsColumnValue)
	{
		if (StringUtils.isNotBlank(enumsColumnValue))
		{
			List<Entity> refEntities = new ArrayList<>();
			for (String eachEnumValue : enumsColumnValue.substring(2, enumsColumnValue.length() - 2)
					.split(CATEGORY_SEPARATOR))
			{
				Matcher matcher = CATEGORY_REGEX.matcher(eachEnumValue);
				if (matcher.find())
				{
					String code = matcher.group(1);
					String label = matcher.group(2);
					refEntities.add(new MapEntity(ImmutableMap.of(CATEGORY_CODE, code, CATEGORY_LABEL, label)));
				}
			}
			return refEntities;
		}
		return Collections.emptyList();
	}

	String mapFinnishDataTypeToEmxDataType(Entity next)
	{
		String finnishDataType = next.getString(ATTRIBUTE_DATATYPE_COLUMN);

		if (StringUtils.isNotBlank(next.getString(ATTRIBUTE_ENUMS_COLUMN)))
		{
			return MolgenisFieldTypes.CATEGORICAL.toString();
		}

		switch (finnishDataType)
		{
			case "int":
				return "int";
			case "double":
				return "decimal";
			case "string":
			default:
				return "string";
		}
	}

	Entity createEntityEntry(String packageName, String entityName)
	{
		MapEntity entity = new MapEntity();
		entity.set("package", packageName);
		entity.set("name", entityName);
		entity.set("backend", "ElasticSearch");
		return entity;
	}

	Entity createIdAttribute(String entityFullName)
	{
		MapEntity entity = new MapEntity();
		entity.set(AttributeMetaDataMetaData.NAME, "id");
		entity.set("entity", entityFullName);
		entity.set(AttributeMetaDataMetaData.DATA_TYPE, MolgenisFieldTypes.STRING.toString());
		entity.set(AttributeMetaDataMetaData.NILLABLE, false);
		entity.set(AttributeMetaDataMetaData.ID_ATTRIBUTE, "AUTO");
		entity.set(AttributeMetaDataMetaData.UNIQUE, false);
		return entity;
	}

	String createRefEntityName(String entityName, String attributeName)
	{
		return (entityName + " " + attributeName).replaceAll(" ", "_");
	}

	String createEntityName(String packageName, String entityName)
	{
		return (packageName + " " + entityName).replaceAll(" ", "_");
	}
}
