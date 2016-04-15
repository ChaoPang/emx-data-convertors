package snmoedct2emx.convertor.beans;

public class IdentifiableNodePath
{
	private final String nodePath;
	private final boolean root;
	private final String id;

	public IdentifiableNodePath(String id, boolean root, String nodePath)
	{
		this.id = id;
		this.root = root;
		this.nodePath = nodePath;
	}

	public boolean isRoot()
	{
		return root;
	}

	public String getNodePath()
	{
		return nodePath;
	}

	public String getId()
	{
		return id;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		IdentifiableNodePath other = (IdentifiableNodePath) obj;
		if (id == null)
		{
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}
