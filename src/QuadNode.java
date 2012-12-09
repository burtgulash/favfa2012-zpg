public class QuadNode {
	QuadNode parent;
	Terrain tree;
	int depth;
	int position;
	int nodeSize;
	boolean hasChildren;
	boolean isActive;
	boolean isSplit;

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
		if (isSplit) {
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

	public void enforceMinimumDepth() {
		if (depth < tree.MIN_DEPTH) {
			if (hasChildren) {
				isActive = false;
				isSplit = true;

				childTopLeft.enforceMinimumDepth();
				childTopRight.enforceMinimumDepth();
				childBottomLeft.enforceMinimumDepth();
				childBottomRight.enforceMinimumDepth();
			} else {
				activate();
				isSplit = false;
			}

			return;
		}

		if (depth == tree.MIN_DEPTH) {
			activate();
			isSplit = false;
		}
	}
}
