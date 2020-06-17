package forge.game.research;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DoublyLinkedList<Item> implements Iterable<Item>
{
    //
    // Node helper class facilitating dual-directional pointing
    //
    private class Node
    {
        private Item item;
        private Node next;
        private Node prev;

        public Node(Node prev, Item item, Node next)
        {
            prev = prev;
            item = item;
            next = next;
        }
    }


    public boolean contains(Item target)
    {
        for (Item item : this)
        {
            if (item.equals(target)) return true;
        }

        return false;
    }

    private int size;     // number of elements on list
    private Node head;    // sentinel before first item
    private Node tail;    // sentinel after last item

    public DoublyLinkedList() {
        head = new Node(null, null, null);
        tail = new Node(head, null, null);

        head.next = tail;}
    public boolean isEmpty()    { return head.next == tail; }
    public int size()           { return size;}

    public void clear()
    {
        while (!isEmpty()) popFront();
    }

    /**
     * Add an element to the tail of the linked list: O(1) operation
     * @param item
     */
    public void pushFront(Item item)
    {
        insert(head, item, head.next);
    }

    /**
     * Add an element to the tail of the linked list: O(1) operation
     * @param /data
     */
    public void push_back(Item item)
    {
        insert(tail.prev, item, tail);
    }

    public Item peek_front() { return head.next.item; }
    public Item peek_back() { return tail.prev.item; }
    /**
     * Insert item between left and right nodes
     * @param left -- a node
     * @param item -- a user-provided item
     * @param right -- a node
     */
    private void insert(Node left, Item item, Node right)
    {
        left.next = right.prev = new Node(left, item, right);
        size++;
    }
    
    /**
     * Delete a node and return the next node in the list
     * @param n -- a node
     * @return
     */
    private Node deleteNode(Node n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
        size--;

        return n.next;
    }
    /**
     * Remove the first element and return it
     * @return an item
     * @throws NoSuchElementException if the list is empty
     */
    public Item popFront() throws NoSuchElementException
    {
        if (isEmpty()) throw new NoSuchElementException();

        Item item = head.next.item;

        // Delete the first valid node containing data
        deleteNode(head.next);

        return item;
    }
    /**
     * Remove the last element and return it
     * @return an item
     * @throws NoSuchElementException if the list is empty
     */
    public Item pop_back() throws NoSuchElementException
    {
        if (isEmpty()) throw new NoSuchElementException();

        Item item = tail.prev.item;

        // Delete the first valid node containing data
        deleteNode(tail.prev);

        return item;
    }

    /**
     * Returns an iterator pointing at the first element in the list
     */
    public Iterator<Item> iterator()  { return new DoublyLinkedListIterator(); }
    //
    // Assumes no calls to methods that mutate the list during iteration: push and pop
    //
    private class DoublyLinkedListIterator implements Iterator<Item> {
        private Node current;         // the node that is returned by next()
        private int index = 0;        // 0-based index for list traversal

        // public DoublyLinkedListIterator()
        {
            current = head.next; // The 'first' element in the list
            index = 0;
        }

        public boolean hasNext() {
            return index < size;
        }

        /**
         * Returns the next element in the list and advances the cursor position
         * (postfix iteration)
         *
         * @ return -- an item
         */
        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            Item item = current.item;
            current = current.next;
            index++;

            return item;
        }
    }
    /**
     * @return String representation of the contents of the list
     * This class is Iterable hence we can use the enhanced for-loop
     */
    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (Item item : this)
            s.append(item + " ");
        return s.toString();
    }
}