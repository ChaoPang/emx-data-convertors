package org.finnish.data.convertor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;

public class CategoryRefEntityMetaData extends DefaultEntityMetaData
{
	private static final String CODE = "code";
	private static final String LABEL = "label";

	public CategoryRefEntityMetaData(String simpleName)
	{
		super(simpleName);
		addAttributeMetaData(
				new DefaultAttributeMetaData(CODE).setVisible(true).setIdAttribute(true).setNillable(false));
		addAttributeMetaData(
				new DefaultAttributeMetaData(LABEL).setVisible(true).setLabelAttribute(true).setNillable(false));
	}

	public List<String> getAttributeNames()
	{
		return StreamSupport.stream(getAtomicAttributes().spliterator(), false).map(AttributeMetaData::getName)
				.collect(Collectors.toList());
	}

	public Entity getAttributeEntityForCode()
	{
		AttributeMetaData attribute = getAttribute(CODE);
		return createAttributeEntity(attribute);
	}

	public Entity getAttributeEntityForLabel()
	{
		AttributeMetaData attribute = getAttribute(LABEL);
		return createAttributeEntity(attribute);
	}

	private Entity createAttributeEntity(AttributeMetaData attribute)
	{
		MapEntity attributeEntity = new MapEntity();
		attributeEntity.set(AttributeMetaDataMetaData.NAME, attribute.getName());
		attributeEntity.set(AttributeMetaDataMetaData.DATA_TYPE, attribute.getDataType());
		attributeEntity.set("entity", getName());
		attributeEntity.set(AttributeMetaDataMetaData.ID_ATTRIBUTE, attribute.isIdAtrribute());
		attributeEntity.set(AttributeMetaDataMetaData.NILLABLE, attribute.isNillable());
		attributeEntity.set(AttributeMetaDataMetaData.LOOKUP_ATTRIBUTE, attribute.isLookupAttribute());
		attributeEntity.set(AttributeMetaDataMetaData.LABEL_ATTRIBUTE, attribute.isLabelAttribute());
		return attributeEntity;
	}
}
