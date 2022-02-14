package com.wjy.simple.lfu;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author weijiayu
 * @date 2022/2/14 17:52
 */
public class LFUCache {

    private int size;
    private int capacity;
    // k=访问次数，v=双端队列头节点
    private TreeMap<Integer, Node> useCountDeQueMap;
    // k=访问次数，v=队列长度
    private HashMap<Integer, Integer> dequeLengthMap;
    // k=数据key，v=队列节点
    private HashMap<Integer, Node> nodeMap;

    public LFUCache(int capacity) {
        this.size = 0;
        this.capacity = capacity;
        // 最小堆
        useCountDeQueMap = new TreeMap();
        // 各队列长度
        dequeLengthMap = new HashMap<>();
        nodeMap = new HashMap<>();
    }

    public int get(int key) {
        if (capacity == 0) {
            return -1;
        }
        Node node = nodeMap.get(key);
        if (node == null) {
            return -1;
        } else {
            flushNode(node);
            return node.val;
        }
    }

    public void put(int key, int value) {
        if (capacity == 0) {
            return;
        }
        Node node = nodeMap.get(key);
        if (node != null) {
            // 节点已存在，更新节点
            node.val = value;
            flushNode(node);
            return;
        }
        // 新节点
        if (size < capacity) {
            // 容量未满
            size++;
        } else {
            // 容量已满：找到最小使用次数的队列，删除最久未使用节点
            // 最小堆，根节点为最小使用次数
            Map.Entry<Integer, Node> minUseDequeEntry = useCountDeQueMap.firstEntry();
            int minKey = removeHead(minUseDequeEntry.getValue());
            nodeMap.remove(minKey);
        }
        node = new Node();
        node.key = key;
        node.val = value;
        // 使用次数初始值为1
        node.useCount = 1;
        nodeMap.put(key, node);
        // 根据使用次数查找节点所在的队列
        Node headNode = useCountDeQueMap.get(node.useCount);
        if (headNode == null) {
            // 队列不存在则创建新队列
            headNode = newDeque(node.useCount);
        }
        // 新节点插入到队尾
        add2Tail(headNode, node);
    }

    private Node newDeque(int useCount) {
        Node headNode = new Node();
        Node tailNode = new Node();
        headNode.preNode = tailNode;
        headNode.nextNode = tailNode;
        tailNode.preNode = headNode;
        tailNode.nextNode = headNode;
        // 队列信息初始化
        useCountDeQueMap.put(useCount, headNode);
        dequeLengthMap.put(useCount, 0);
        return headNode;
    }

    // 队尾插入节点
    private void add2Tail(Node headNode, Node node) {
        // 利用双端队列，根据头结点找到尾节点
        Node tailNode = headNode.preNode;
        // 在尾部哨兵节点前插入新节点
        node.preNode = tailNode.preNode;
        node.nextNode = tailNode;
        tailNode.preNode.nextNode = node;
        tailNode.preNode = node;
        // 队列长度+1
        dequeLengthMap.put(node.useCount, dequeLengthMap.get(node.useCount) + 1);
    }

    // 从队列里移动节点到队尾
    private void move2Tail(Node headNode, Node node) {
        // 清除原来相邻节点关系
        node.preNode.nextNode = node.nextNode;
        node.nextNode.preNode = node.preNode;
        add2Tail(headNode, node);
    }

    // 清除原来相邻节点关系，但不删除节点
    private void remove(Node node) {
        node.preNode.nextNode = node.nextNode;
        node.nextNode.preNode = node.preNode;
        // 队列长度-1
        dequeLengthMap.put(node.useCount, dequeLengthMap.get(node.useCount) - 1);
        if (dequeLengthMap.get(node.useCount) <= 0) {
            // 除去头尾哨兵节点，队列实际长度为空，删除队列
            dequeLengthMap.remove(node.useCount);
            useCountDeQueMap.remove(node.useCount);
        }
    }

    // 删除队首节点
    private int removeHead(Node headNode) {
        // 跳过队首哨兵节点，找到第一个节点
        Node node = headNode.nextNode;
        int key = node.key;
        node.nextNode.preNode = headNode;
        headNode.nextNode = node.nextNode;
        // 队列长度-1
        dequeLengthMap.put(node.useCount, dequeLengthMap.get(node.useCount) - 1);
        if (dequeLengthMap.get(node.useCount) <= 0) {
            // 除去头尾哨兵节点，队列实际长度为空，删除队列
            dequeLengthMap.remove(node.useCount);
            useCountDeQueMap.remove(node.useCount);
        }
        return key;
    }

    // get或put已有节点后更新
    private void flushNode(Node node) {
        // 由于使用次数+1，离开原来的队列
        remove(node);
        // 使用次数+1
        node.useCount++;
        // 从旧队列移动新队列尾部。先找到新队列
        Node headNode = useCountDeQueMap.get(node.useCount);
        if (headNode == null) {
            headNode = newDeque(node.useCount);
            // 插入到队列尾部
            add2Tail(headNode, node);
        } else {
            // 插入到队列尾部
            move2Tail(headNode, node);
        }
    }

    private class Node {
        private int key;
        private int val;
        private int useCount;
        private Node preNode;
        private Node nextNode;
    }
}
