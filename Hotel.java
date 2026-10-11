import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Hotel Management System (console based).
 *
 * Compile : javac Hotel.java
 * Run     : java Hotel
 */

/** Exception used to report problems (hotel full, customer not found, ...). */
class HotelException extends Exception {
    private static final long serialVersionUID = 1L;

    public HotelException(String message) {
        super(message);
    }
}

/** Base class: any person known to the hotel. */
class Person {
    private String name;

    public Person(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

/** One hotel room together with the customer staying in it (if any). */
class Customer extends Person {
    private final int room;
    private boolean occupied;

    public Customer(int room) {
        super("");
        this.room = room;
        this.occupied = false;
    }

    public int getRoom() {
        return room;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void checkIn(String name) {
        setName(name);
        occupied = true;
    }

    public void checkOut() {
        setName("");
        occupied = false;
    }
}

public class Hotel {
    private static final int TOTAL_ROOMS = 10;

    private final Customer[] rooms;

    public Hotel(int totalRooms) {
        rooms = new Customer[totalRooms];
        for (int i = 0; i < rooms.length; i++) {
            rooms[i] = new Customer(i + 1);
        }
    }

    /** Prints every room and whether it is vacant or occupied. */
    public void viewRooms() {
        for (Customer c : rooms) {
            if (c.isOccupied()) {
                System.out.println("Room number " + c.getRoom()
                        + " is occupied by " + c.getName() + ".");
            } else {
                System.out.println("Room number " + c.getRoom() + " is vacant.");
            }
        }
        System.out.println();
    }

    /** Gives the first vacant room to the customer and returns its number. */
    public int addCustomer(String name) throws HotelException {
        if (findIndex(name) != -1) {
            throw new HotelException("A customer named " + name
                    + " is already checked in.");
        }
        for (Customer c : rooms) {
            if (!c.isOccupied()) {
                c.checkIn(name);
                return c.getRoom();
            }
        }
        throw new HotelException("No rooms available!");
    }

    /** Prints the numbers of all vacant rooms. */
    public void showEmptyRooms() {
        System.out.println("Available rooms are:");
        boolean any = false;
        for (Customer c : rooms) {
            if (!c.isOccupied()) {
                System.out.println(c.getRoom());
                any = true;
            }
        }
        if (!any) {
            System.out.println("None - the hotel is full.");
        }
        System.out.println();
    }

    /** Checks the customer out and returns the room that became vacant. */
    public int deleteCustomer(String name) throws HotelException {
        int index = findIndex(name);
        if (index == -1) {
            throw new HotelException("No customer named " + name + " was found.");
        }
        rooms[index].checkOut();
        return rooms[index].getRoom();
    }

    /** Returns the room number occupied by the given customer. */
    public int findRoom(String name) throws HotelException {
        int index = findIndex(name);
        if (index == -1) {
            throw new HotelException("No customer named " + name + " was found.");
        }
        return rooms[index].getRoom();
    }

    /** Index of the occupied room whose customer has this name, or -1. */
    private int findIndex(String name) {
        for (int i = 0; i < rooms.length; i++) {
            if (rooms[i].isOccupied() && rooms[i].getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private static void printMenu() {
        System.out.println("          Hotel Booking Options");
        System.out.println("=====================================");
        System.out.println("1: To View all rooms");
        System.out.println("2: To Add customer to a room");
        System.out.println("3: To Display empty rooms");
        System.out.println("4: To Delete customer from a room");
        System.out.println("5: Find room from customer name");
        System.out.println("0: Exit");
        System.out.print("\nEnter your choice: ");
    }

    private static String readName(Scanner input) throws HotelException {
        System.out.print("Customer's name: ");
        String name = input.nextLine().trim();
        System.out.println();
        if (name.isEmpty()) {
            throw new HotelException("Name cannot be empty.");
        }
        return name;
    }

    public static void main(String[] args) {
        Hotel hotel = new Hotel(TOTAL_ROOMS);
        Scanner input = new Scanner(System.in);
        int option = -1;

        do {
            printMenu();
            try {
                option = Integer.parseInt(input.nextLine().trim());
                System.out.println();

                switch (option) {
                    case 1:
                        hotel.viewRooms();
                        break;
                    case 2: {
                        String name = readName(input);
                        int room = hotel.addCustomer(name);
                        System.out.println("Room " + room + " allotted to " + name + ".\n");
                        break;
                    }
                    case 3:
                        hotel.showEmptyRooms();
                        break;
                    case 4: {
                        String name = readName(input);
                        int room = hotel.deleteCustomer(name);
                        System.out.println("Deletion successful. Room " + room
                                + " is now vacant.\n");
                        break;
                    }
                    case 5: {
                        String name = readName(input);
                        System.out.println("Customer's room: " + hotel.findRoom(name) + "\n");
                        break;
                    }
                    case 0:
                        System.out.println("Thank you!\n");
                        break;
                    default:
                        System.out.println("Invalid option! Please choose 0 to 5.\n");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("\nInvalid input! Please enter a number from 0 to 5.\n");
            } catch (HotelException e) {
                System.out.println(e.getMessage() + "\n");
            } catch (NoSuchElementException e) {
                System.out.println("\nInput closed. Exiting.");
                option = 0;
            }
        } while (option != 0);

        input.close();
    }
}
