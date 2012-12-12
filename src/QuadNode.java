public class QuadNode {
	QuadNode parent;
	Terrain tree;
	int depth;
	int position;
	int nodeSize;
	boolean hasChildren;
	boolean isActive;
	boolean isSplit;
	Bounds bounds;

	NodeType nodeType;

	QuadVertex vertexTopLeft;
	QuadVertex vertexTop;
	QuadVertex vertexTopRight;
	QuadVertex vertexLeft;
	QuadVertex vertexCenter;
	QuadVertex vertexRight;
	QuadVertex vertexBottomLeft;
	QuadVertex vertexBottom;
	QuadVertex vertexBottomRight;

	QuadNode childTopLeft;
	QuadNode childTopRight;
	QuadNode childBottomLeft;
	QuadNode childBottomRight;

	QuadNode neighborTop;
	QuadNode neighborRight;
	QuadNode neighborBottom;
	QuadNode neighborLeft;

	public QuadNode(NodeType nodeType, int depth, int nodeSize, int position,
			QuadNode parent, Terrain tree) {
		this.nodeType = nodeType;
		this.depth = depth;
		this.position = position;
		this.parent = parent;
		this.tree = tree;
		this.nodeSize = nodeSize;

		addVertices();
		bounds = new Bounds(tree.getV(vertexCenter.index));
		bounds.update(tree.getV(vertexTopLeft.index));
		bounds.update(tree.getV(vertexTop.index));
		bounds.update(tree.getV(vertexTopRight.index));
		bounds.update(tree.getV(vertexRight.index));
		bounds.update(tree.getV(vertexBottomRight.index));
		bounds.update(tree.getV(vertexBottom.index));
		bounds.update(tree.getV(vertexBottomLeft.index));
		bounds.update(tree.getV(vertexLeft.index));
		bounds.update(tree.getV(vertexCenter.index));

		if (!isLeaf())
			addChildren();
		

		// By updating neighbors recursively from root we ensure that all nodes
		// already exist.
		if (isRoot()) {
			addNeighbors();
			activate();
		}
	}

	public boolean isRoot() {
		return depth == tree.ROOT_DEPTH;
	}

	public boolean isLeaf() {
		return nodeSize < 4;
	}

	public boolean canSplit() {
		return nodeSize >= 2;
	}

	public void split() {
		if (tree.CULLING_ENABLED && !isInView())
			return;

		if (parent != null && !parent.isSplit)
			parent.split();

		if (canSplit()) {
			if (hasChildren) {
				childTopLeft.activate();
				childTopRight.activate();
				childBottomLeft.activate();
				childBottomRight.activate();
			}

			isActive = !hasChildren;
			isSplit = true;
			vertexTop.activated = true;
			vertexRight.activated = true;
			vertexBottom.activated = true;
			vertexLeft.activated = true;

			fixNeighbors();
		}
	}

	private boolean isInView() {
		return bounds.isInView();
	}

	public void setDepth(int d) {
		if (tree.CULLING_ENABLED && !isInView())
			return;

		if (depth > d)
			return;

		if (canSplit()) {
			vertexTop.activated = true;
			vertexRight.activated = true;
			vertexBottom.activated = true;
			vertexLeft.activated = true;
			isSplit = true;

			if (hasChildren && depth < d) {
				childTopLeft.setDepth(d);
				childTopRight.setDepth(d);
				childBottomRight.setDepth(d);
				childBottomLeft.setDepth(d);
			} else {
				activate();
				fixNeighbors();
			}
		}
	}

	private void fixNeighbors() {
		ensureNeighborParentSplit(neighborTop);
		ensureNeighborParentSplit(neighborRight);
		ensureNeighborParentSplit(neighborBottom);
		ensureNeighborParentSplit(neighborLeft);

		if (neighborTop != null)
			neighborTop.vertexBottom.activated = true;
		if (neighborRight != null)
			neighborRight.vertexLeft.activated = true;
		if (neighborBottom != null)
			neighborBottom.vertexTop.activated = true;
		if (neighborLeft != null)
			neighborLeft.vertexRight.activated = true;
	}

	private void ensureNeighborParentSplit(QuadNode neighbor) {
		if (neighbor != null && neighbor.parent != null)
			if (!neighbor.parent.isSplit)
				neighbor.parent.split();
	}

	public void merge() {
		vertexTop.activated = false;
		vertexRight.activated = false;
		vertexBottom.activated = false;
		vertexLeft.activated = false;

		if (!isRoot()) {
			vertexTopLeft.activated = false;
			vertexTopRight.activated = false;
			vertexBottomRight.activated = false;
			vertexBottomLeft.activated = false;
		}

		isSplit = false;
		isActive = true;

		if (hasChildren) {
			if (childTopLeft.isSplit) {
				childTopLeft.merge();
				childTopLeft.isActive = false;
			} else {
				childTopLeft.vertexTop.activated = false;
				childTopLeft.vertexRight.activated = false;
				childTopLeft.vertexBottom.activated = false;
				childTopLeft.vertexLeft.activated = false;
			}

			if (childTopRight.isSplit) {
				childTopRight.merge();
				childTopRight.isActive = false;
			} else {
				childTopRight.vertexTop.activated = false;
				childTopRight.vertexRight.activated = false;
				childTopRight.vertexBottom.activated = false;
				childTopRight.vertexLeft.activated = false;
			}

			if (childBottomRight.isSplit) {
				childBottomRight.merge();
				childBottomRight.isActive = false;
			} else {
				childBottomRight.vertexTop.activated = false;
				childBottomRight.vertexRight.activated = false;
				childBottomRight.vertexBottom.activated = false;
				childBottomRight.vertexLeft.activated = false;
			}

			if (childBottomLeft.isSplit) {
				childBottomLeft.merge();
				childBottomLeft.isActive = false;
			} else {
				childBottomLeft.vertexTop.activated = false;
				childBottomLeft.vertexRight.activated = false;
				childBottomLeft.vertexBottom.activated = false;
				childBottomLeft.vertexLeft.activated = false;
			}
		}
	}

	private void activate() {
		vertexTopLeft.activated = true;
		vertexTopRight.activated = true;
		vertexCenter.activated = true;
		vertexBottomLeft.activated = true;
		vertexBottomRight.activated = true;

		isActive = true;
	}

	private void addVertices() {
		switch (nodeType) {
		case TOP_LEFT:
			vertexTopLeft = parent.vertexTopLeft;
			vertexTopRight = parent.vertexTop;
			vertexBottomLeft = parent.vertexLeft;
			vertexBottomRight = parent.vertexCenter;
			break;

		case TOP_RIGHT:
			vertexTopLeft = parent.vertexTop;
			vertexTopRight = parent.vertexTopRight;
			vertexBottomLeft = parent.vertexCenter;
			vertexBottomRight = parent.vertexRight;
			break;

		case BOTTOM_LEFT:
			vertexTopLeft = parent.vertexLeft;
			vertexTopRight = parent.vertexCenter;
			vertexBottomLeft = parent.vertexBottomLeft;
			vertexBottomRight = parent.vertexBottom;
			break;

		case BOTTOM_RIGHT:
			vertexTopLeft = parent.vertexCenter;
			vertexTopRight = parent.vertexRight;
			vertexBottomLeft = parent.vertexBottom;
			vertexBottomRight = parent.vertexBottomRight;
			break;

		default:
			vertexTopLeft = new QuadVertex(true, 0);
			vertexTopRight = new QuadVertex(true, nodeSize);
			vertexBottomLeft = new QuadVertex(true, (tree.rootNodeSize + 1)
					* nodeSize);
			vertexBottomRight = new QuadVertex(true, vertexBottomLeft.index
					+ nodeSize);
		}

		vertexTop = new QuadVertex(false, vertexTopLeft.index + nodeSize / 2);
		vertexLeft = new QuadVertex(false, vertexTopLeft.index
				+ (tree.rootNodeSize + 1) * nodeSize / 2);
		vertexCenter = new QuadVertex(false, vertexLeft.index + nodeSize / 2);
		vertexRight = new QuadVertex(false, vertexLeft.index + nodeSize);
		vertexBottom = new QuadVertex(false, vertexBottomLeft.index + nodeSize
				/ 2);
	}

	private void addChildren() {
		int d = depth + 1;
		int s = nodeSize / 2;
		childTopLeft = new QuadNode(NodeType.TOP_LEFT, d, s,
				vertexTopLeft.index, this, tree);
		childTopRight = new QuadNode(NodeType.TOP_RIGHT, d, s, vertexTop.index,
				this, tree);
		childBottomLeft = new QuadNode(NodeType.BOTTOM_LEFT, d, s,
				vertexLeft.index, this, tree);
		childBottomRight = new QuadNode(NodeType.BOTTOM_RIGHT, d, s,
				vertexCenter.index, this, tree);
		
		bounds.update(childTopLeft.bounds);
		bounds.update(childTopRight.bounds);
		bounds.update(childBottomLeft.bounds);
		bounds.update(childBottomRight.bounds);
		
		hasChildren = true;
	}

	private void addNeighbors() {
		switch (nodeType) {
		case TOP_LEFT:
			if (parent.neighborTop != null)
				neighborTop = parent.neighborTop.childBottomLeft;

			neighborRight = parent.childTopRight;
			neighborBottom = parent.childBottomLeft;

			if (parent.neighborLeft != null)
				neighborLeft = parent.neighborLeft.childTopRight;

			break;

		case TOP_RIGHT:
			if (parent.neighborTop != null)
				neighborTop = parent.neighborTop.childBottomRight;

			if (parent.neighborRight != null)
				neighborRight = parent.neighborRight.childTopLeft;

			neighborBottom = parent.childBottomRight;
			neighborLeft = parent.childTopLeft;

			break;

		case BOTTOM_LEFT:
			neighborTop = parent.childTopLeft;
			neighborRight = parent.childBottomRight;

			if (parent.neighborBottom != null)
				neighborBottom = parent.neighborBottom.childTopLeft;

			if (parent.neighborLeft != null)
				neighborLeft = parent.neighborLeft.childBottomRight;

			break;

		case BOTTOM_RIGHT:
			neighborTop = parent.childTopRight;

			if (parent.neighborRight != null)
				neighborRight = parent.neighborRight.childBottomLeft;

			if (parent.neighborBottom != null)
				neighborBottom = parent.neighborBottom.childTopRight;

			neighborLeft = parent.childBottomLeft;

			break;
		default:
			break;
		}

		if (hasChildren) {
			childTopLeft.addNeighbors();
			childTopRight.addNeighbors();
			childBottomLeft.addNeighbors();
			childBottomRight.addNeighbors();
		}
	}

	void setActiveVertices() {
		if (tree.CULLING_ENABLED && !isInView())
			return;

		if (isSplit && hasChildren) {
			childTopLeft.setActiveVertices();
			childTopRight.setActiveVertices();
			childBottomLeft.setActiveVertices();
			childBottomRight.setActiveVertices();
			return;
		}

		tree.pushIndex(vertexCenter.index);
		tree.pushIndex(vertexTopLeft.index);
		if (vertexTop.activated) {
			tree.pushIndex(vertexTop.index);
			tree.pushIndex(vertexCenter.index);
			tree.pushIndex(vertexTop.index);
		}
		tree.pushIndex(vertexTopRight.index);

		tree.pushIndex(vertexCenter.index);
		tree.pushIndex(vertexTopRight.index);
		if (vertexRight.activated) {
			tree.pushIndex(vertexRight.index);
			tree.pushIndex(vertexCenter.index);
			tree.pushIndex(vertexRight.index);
		}
		tree.pushIndex(vertexBottomRight.index);

		tree.pushIndex(vertexCenter.index);
		tree.pushIndex(vertexBottomRight.index);
		if (vertexBottom.activated) {
			tree.pushIndex(vertexBottom.index);
			tree.pushIndex(vertexCenter.index);
			tree.pushIndex(vertexBottom.index);
		}
		tree.pushIndex(vertexBottomLeft.index);

		tree.pushIndex(vertexCenter.index);
		tree.pushIndex(vertexBottomLeft.index);
		if (vertexLeft.activated) {
			tree.pushIndex(vertexLeft.index);
			tree.pushIndex(vertexCenter.index);
			tree.pushIndex(vertexLeft.index);
		}
		tree.pushIndex(vertexTopLeft.index);
	}

	public boolean contains(float x, float z) {
		return bounds.contains(x, z);
	}

	public QuadNode nodeWithPointMaxDepth(float x, float z, int maxDepth) {
		if (!contains(x, z))
			return null;

		if (hasChildren && depth < maxDepth) {
			if (childTopLeft.contains(x, z))
				return childTopLeft.nodeWithPointMaxDepth(x, z, maxDepth);
			if (childTopRight.contains(x, z))
				return childTopRight.nodeWithPointMaxDepth(x, z, maxDepth);
			if (childBottomRight.contains(x, z))
				return childBottomRight.nodeWithPointMaxDepth(x, z, maxDepth);
			return childBottomLeft.nodeWithPointMaxDepth(x, z, maxDepth);
		}

		return this;
	}

	public QuadNode deepestNodeWithPoint(float x, float z) {
		return nodeWithPointMaxDepth(x, z, Integer.MAX_VALUE);
	}
}