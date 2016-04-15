package snmoedct2emx.convertor.beans;

import snmoedct2emx.convertor.io.SnomeCTConceptRelationRepo.TypeEnum;

public class SnomedCTSubClassRelation extends SnomedCTAbstractRelation
{
	private final static TypeEnum SUBCLASS_TYPE_ENUM = TypeEnum.IS_A;

	public SnomedCTSubClassRelation(String id, boolean active, String sourceId, String destionationId)
	{
		super(id, active, sourceId, SUBCLASS_TYPE_ENUM, destionationId);
	}
}
