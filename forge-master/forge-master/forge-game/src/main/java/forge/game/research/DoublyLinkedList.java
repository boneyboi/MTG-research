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
        private Item _item;
        private Node _next;
        private Node _prev;

        public Node(Node prev, Item item, Node next)
        {
            _prev = prev;
            _item = item;
            _next = next;
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

    private int _size;     // number of elements on list
    private Node _head;    // sentinel before first item
    private Node _tail;    // sentinel after last item

    public DoublyLinkedList() {
        _head = new Node(null, null, null);
        _tail = new Node(_head, null, null);

        _head._next = _tail;
    }
    public boolean isEmpty()    { return _head._next == _tail; }
    public int size()           { return _size;      }

    public void clear()
    {
        while (!isEmpty()) pop_front();
    }

    /**
     * Add an element to the tail of the linked list: O(1) operation
     * @param data
     */
    public void push_front(Item item)
    {
        insert(_head, item, _head._next);
    }

    /**
     * Add an element to the tail of the linked list: O(1) operation
     * @param data
     */
    public void push_back(Item item)
    {
        insert(_tail._prev, item, _tail);
    }

    public Item peek_front() { return _head._next._item; }
    public Item peek_back() { return _tail._prev._item; }
    

    /**
     * Insert item between left and right nodes
     * @param left -- a node
     * @param item -- a user-provided item
     * @param right -- a node
     */
    private void insert(Node left, Item item, Node right)
    {
        left._next = right._prev = new Node(left, item, right);
        _size++;
    }

    /**
     * Delete a node and return the next node in the list
     * @param n -- a node
     * @return
     */
    private Node deleteNode(Node n)
    {
        n._prev._next = n._next;
        n._next._prev = n._prev;
        _size--;

        return n._next;
    }

    /**
     * Remove the first element and return it
     * @return an item
     * @throws NoSuchElementException if the list is empty
     */
    public Item pop_front() throws NoSuchElementException
    {
        if (isEmpty()) throw new NoSuchElementException();

        Item item = _head._next._item;

        // Delete the first valid node containing data
        deleteNode(_head._next);

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

        Item item = _tail._prev._item;

        // Delete the first valid node containing data
        deleteNode(_tail._prev);

        return item;
    }

    /**
     * Returns an iterator pointing at the first element in the list
     */
    public Iterator<Item> iterator()  { return new DoublyLinkedListIterator(); }

    //
    // Assumes no calls to methods that mutate the list during iteration: push and pop
    //
    private class DoublyLinkedListIterator implements Iterator<Item>
    {
        private Node __current;         // the node that is returned by next()
        private int __index = 0;        // 0-based index for list traversal

        public DoublyLinkedListIterator()
        {
            __current = _head._next; // The 'first' element in the list
            __index = 0;
        }
        
        public boolean hasNext()      { return __index < _size; }

        /**
         * Returns the next element in the list and advances the cursor position
         * (postfix iteration)
         * @ return -- an item
         */
        public Item next()
        {
            if (!hasNext()) throw new NoSuchElementException();

            Item item = __current._item;
            __current = __current._next;
            __index++;

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