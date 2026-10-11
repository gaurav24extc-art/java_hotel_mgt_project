import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Blood Donation & Blood Bank Management System
 * Java Programming - Pillai College of Engineering
 *
 * Layers (as in the report):
 *   User Interface  -> BloodBankManagementSystem (console menu)
 *   Service Layer   -> BloodBankService
 *   Data Layer      -> DataStore (CSV file storage)
 *
 * Compile: javac BloodBankManagementSystem.java
 * Run    : java BloodBankManagementSystem
 *
 * Default logins:  admin / admin123   operator / op123   hospital / req123
 * NOTE: Academic prototype only. No medical screening or compatibility testing.
 */

// ===================== Enums & Exception =====================

enum Role { ADMIN, OPERATOR, REQUESTER }

enum RequestStatus { PENDING, ISSUED, REJECTED }

class BloodBankException extends Exception {
    public BloodBankException(String message) { super(message); }
}

// ===================== Entity Classes =====================

class User {
    private final String userId, username, password;
    private final Role role;

    User(String userId, String username, String password, Role role) {
        this.userId = userId; this.username = username;
        this.password = password; this.role = role;
    }
    String getUserId() { return userId; }
    String getUsername() { return username; }
    Role getRole() { return role; }
    boolean checkPassword(String p) { return password.equals(p); }
    String toCsv() { return userId + "|" + username + "|" + password + "|" + role; }
}

class Donor {
    private final String donorId, name, contact, bloodGroup;
    private final int age;

    Donor(String donorId, String name, int age, String contact, String bloodGroup) {
        this.donorId = donorId; this.name = name; this.age = age;
        this.contact = contact; this.bloodGroup = bloodGroup;
    }
    String getDonorId() { return donorId; }
    String getName() { return name; }
    int getAge() { return age; }
    String getContact() { return contact; }
    String getBloodGroup() { return bloodGroup; }
    String toCsv() { return donorId + "|" + name + "|" + age + "|" + contact + "|" + bloodGroup; }
}

class Donation {
    private final String donationId, donorId, bloodGroup, status;
    private final LocalDate donationDate;
    private final int units;

    Donation(String donationId, String donorId, LocalDate date, String bloodGroup, int units, String status) {
        this.donationId = donationId; this.donorId = donorId; this.donationDate = date;
        this.bloodGroup = bloodGroup; this.units = units; this.status = status;
    }
    String getDonationId() { return donationId; }
    String getDonorId() { return donorId; }
    LocalDate getDonationDate() { return donationDate; }
    String getBloodGroup() { return bloodGroup; }
    int getUnits() { return units; }
    String getStatus() { return status; }
    String toCsv() {
        return donationId + "|" + donorId + "|" + donationDate + "|" + bloodGroup + "|" + units + "|" + status;
    }
}

class BloodStock {
    private final String bloodGroup;
    private int availableUnits;
    private LocalDate lastUpdated;

    BloodStock(String bloodGroup, int availableUnits, LocalDate lastUpdated) {
        this.bloodGroup = bloodGroup; this.availableUnits = availableUnits; this.lastUpdated = lastUpdated;
    }
    String getBloodGroup() { return bloodGroup; }
    int getAvailableUnits() { return availableUnits; }
    LocalDate getLastUpdated() { return lastUpdated; }

    // Updated Stock = Previous Stock + Units Added
    void addUnits(int units) {
        availableUnits += units;
        lastUpdated = LocalDate.now();
    }

    // Updated Stock = Previous Stock - Units Issued (never negative)
    void removeUnits(int units) throws BloodBankException {
        if (units > availableUnits)
            throw new BloodBankException("Insufficient stock for " + bloodGroup);
        availableUnits -= units;
        lastUpdated = LocalDate.now();
    }

    String getStatus() { return availableUnits <= 5 ? "Limited" : "Available"; }
    String toCsv() { return bloodGroup + "|" + availableUnits + "|" + lastUpdated; }
}

class BloodRequest {
    private final String requestId, requester, bloodGroup;
    private final int unitsRequired;
    private RequestStatus status;

    BloodRequest(String requestId, String requester, String bloodGroup, int unitsRequired, RequestStatus status) {
        this.requestId = requestId; this.requester = requester; this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired; this.status = status;
    }
    String getRequestId() { return requestId; }
    String getRequester() { return requester; }
    String getBloodGroup() { return bloodGroup; }
    int getUnitsRequired() { return unitsRequired; }
    RequestStatus getStatus() { return status; }
    void setStatus(RequestStatus s) { status = s; }
    String toCsv() {
        return requestId + "|" + requester + "|" + bloodGroup + "|" + unitsRequired + "|" + status;
    }
}

class BloodIssue {
    private final String issueId, requestId, approvedBy;
    private final int unitsIssued;
    private final LocalDate issueDate;

    BloodIssue(String issueId, String requestId, int unitsIssued, LocalDate issueDate, String approvedBy) {
        this.issueId = issueId; this.requestId = requestId; this.unitsIssued = unitsIssued;
        this.issueDate = issueDate; this.approvedBy = approvedBy;
    }
    String getIssueId() { return issueId; }
    String getRequestId() { return requestId; }
    int getUnitsIssued() { return unitsIssued; }
    LocalDate getIssueDate() { return issueDate; }
    String getApprovedBy() { return approvedBy; }
    String toCsv() {
        return issueId + "|" + requestId + "|" + unitsIssued + "|" + issueDate + "|" + approvedBy;
    }
}

// ===================== Data Layer =====================

class DataStore {
    static final String[] BLOOD_GROUPS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    final Map<String, User> users = new LinkedHashMap<>();
    final Map<String, Donor> donors = new LinkedHashMap<>();
    final Map<String, Donation> donations = new LinkedHashMap<>();
    final Map<String, BloodStock> stock = new LinkedHashMap<>();
    final Map<String, BloodRequest> requests = new LinkedHashMap<>();
    final Map<String, BloodIssue> issues = new LinkedHashMap<>();

    private final Path dir = Paths.get("bloodbank_data");

    DataStore() {
        try {
            Files.createDirectories(dir);
            load();
        } catch (IOException e) {
            System.out.println("Storage error: " + e.getMessage());
        }
        // Defaults on first run
        if (users.isEmpty()) {
            users.put("U001", new User("U001", "admin", "admin123", Role.ADMIN));
            users.put("U002", new User("U002", "operator", "op123", Role.OPERATOR));
            users.put("U003", new User("U003", "hospital", "req123", Role.REQUESTER));
        }
        for (String g : BLOOD_GROUPS)
            stock.putIfAbsent(g, new BloodStock(g, 0, LocalDate.now()));
        save();
    }

    String nextId(String prefix, Set<String> existing) {
        int max = 0;
        for (String id : existing) {
            try { max = Math.max(max, Integer.parseInt(id.substring(prefix.length()))); }
            catch (NumberFormatException ignored) { }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    private List<String[]> readFile(String name) throws IOException {
        List<String[]> rows = new ArrayList<>();
        Path p = dir.resolve(name);
        if (!Files.exists(p)) return rows;
        for (String line : Files.readAllLines(p)) {
            if (!line.trim().isEmpty()) rows.add(line.split("\\|", -1));
        }
        return rows;
    }

    private void load() throws IOException {
        try {
            for (String[] r : readFile("users.csv"))
                users.put(r[0], new User(r[0], r[1], r[2], Role.valueOf(r[3])));
            for (String[] r : readFile("donors.csv"))
                donors.put(r[0], new Donor(r[0], r[1], Integer.parseInt(r[2]), r[3], r[4]));
            for (String[] r : readFile("donations.csv"))
                donations.put(r[0], new Donation(r[0], r[1], LocalDate.parse(r[2]), r[3], Integer.parseInt(r[4]), r[5]));
            for (String[] r : readFile("stock.csv"))
                stock.put(r[0], new BloodStock(r[0], Integer.parseInt(r[1]), LocalDate.parse(r[2])));
            for (String[] r : readFile("requests.csv"))
                requests.put(r[0], new BloodRequest(r[0], r[1], r[2], Integer.parseInt(r[3]), RequestStatus.valueOf(r[4])));
            for (String[] r : readFile("issues.csv"))
                issues.put(r[0], new BloodIssue(r[0], r[1], Integer.parseInt(r[2]), LocalDate.parse(r[3]), r[4]));
        } catch (RuntimeException e) {
            System.out.println("Warning: data file is corrupted (" + e.getMessage() + ").");
        }
    }

    private <T> void write(String name, Collection<T> items, java.util.function.Function<T, String> f) throws IOException {
        List<String> lines = new ArrayList<>();
        for (T t : items) lines.add(f.apply(t));
        Files.write(dir.resolve(name), lines);
    }

    void save() {
        try {
            write("users.csv", users.values(), User::toCsv);
            write("donors.csv", donors.values(), Donor::toCsv);
            write("donations.csv", donations.values(), Donation::toCsv);
            write("stock.csv", stock.values(), BloodStock::toCsv);
            write("requests.csv", requests.values(), BloodRequest::toCsv);
            write("issues.csv", issues.values(), BloodIssue::toCsv);
        } catch (IOException e) {
            System.out.println("Storage error: " + e.getMessage());
        }
    }
}

// ===================== Service Layer =====================

class BloodBankService {
    private final DataStore db;

    BloodBankService(DataStore db) { this.db = db; }

    private static String clean(String s) { return s.replace("|", " ").replace("\n", " ").trim(); }

    static String normalizeGroup(String g) throws BloodBankException {
        String x = g.trim().toUpperCase().replace('\u2212', '-');
        for (String valid : DataStore.BLOOD_GROUPS)
            if (valid.equals(x)) return x;
        throw new BloodBankException("Invalid blood group: " + g + " (use A+, A-, B+, B-, AB+, AB-, O+, O-)");
    }

    User login(String username, String password) {
        for (User u : db.users.values())
            if (u.getUsername().equals(username) && u.checkPassword(password)) return u;
        return null;
    }

    Donor registerDonor(String name, int age, String contact, String group) throws BloodBankException {
        if (name.trim().isEmpty()) throw new BloodBankException("Name cannot be empty.");
        if (age < 18 || age > 65) throw new BloodBankException("Donor age must be between 18 and 65.");
        if (!contact.matches("\\d{10}")) throw new BloodBankException("Contact must be a 10-digit number.");
        String g = normalizeGroup(group);
        String id = db.nextId("D", db.donors.keySet());
        Donor d = new Donor(id, clean(name), age, contact, g);
        db.donors.put(id, d);
        db.save();
        return d;
    }

    Donation recordDonation(String donorId, int units) throws BloodBankException {
        Donor d = db.donors.get(donorId.trim().toUpperCase());
        if (d == null) throw new BloodBankException("Donor ID not found: " + donorId);
        if (units <= 0 || units > 5) throw new BloodBankException("Units must be between 1 and 5.");
        String id = db.nextId("DN", db.donations.keySet());
        Donation don = new Donation(id, d.getDonorId(), LocalDate.now(), d.getBloodGroup(), units, "ELIGIBLE");
        db.donations.put(id, don);
        db.stock.get(d.getBloodGroup()).addUnits(units);   // stock increases
        db.save();
        return don;
    }

    BloodRequest createRequest(String requester, String group, int units) throws BloodBankException {
        if (requester.trim().isEmpty()) throw new BloodBankException("Requester name cannot be empty.");
        String g = normalizeGroup(group);
        if (units <= 0) throw new BloodBankException("Invalid quantity. Units must be greater than zero.");
        String id = db.nextId("R", db.requests.keySet());
        BloodRequest r = new BloodRequest(id, clean(requester), g, units, RequestStatus.PENDING);
        db.requests.put(id, r);
        db.save();
        return r;
    }

    /** Checks stock, then issues blood after authorized approval. */
    BloodIssue approveAndIssue(String requestId, String approvedBy) throws BloodBankException {
        BloodRequest r = db.requests.get(requestId.trim().toUpperCase());
        if (r == null) throw new BloodBankException("Request ID not found: " + requestId);
        if (r.getStatus() != RequestStatus.PENDING)
            throw new BloodBankException("Request is already " + r.getStatus() + ".");
        BloodStock s = db.stock.get(r.getBloodGroup());
        if (r.getUnitsRequired() > s.getAvailableUnits())
            throw new BloodBankException("Insufficient stock. Available: " + s.getAvailableUnits()
                    + " unit(s); request stays PENDING.");
        s.removeUnits(r.getUnitsRequired());               // stock decreases
        String id = db.nextId("I", db.issues.keySet());
        BloodIssue issue = new BloodIssue(id, r.getRequestId(), r.getUnitsRequired(), LocalDate.now(), approvedBy);
        db.issues.put(id, issue);
        r.setStatus(RequestStatus.ISSUED);
        db.save();
        return issue;
    }

    void rejectRequest(String requestId) throws BloodBankException {
        BloodRequest r = db.requests.get(requestId.trim().toUpperCase());
        if (r == null) throw new BloodBankException("Request ID not found: " + requestId);
        if (r.getStatus() != RequestStatus.PENDING)
            throw new BloodBankException("Request is already " + r.getStatus() + ".");
        r.setStatus(RequestStatus.REJECTED);
        db.save();
    }

    User addUser(String username, String password, Role role) throws BloodBankException {
        if (username.trim().isEmpty() || password.length() < 4)
            throw new BloodBankException("Username required and password must be at least 4 characters.");
        for (User u : db.users.values())
            if (u.getUsername().equalsIgnoreCase(username.trim()))
                throw new BloodBankException("Username already exists.");
        String id = db.nextId("U", db.users.keySet());
        User u = new User(id, clean(username), clean(password), role);
        db.users.put(id, u);
        db.save();
        return u;
    }

    // ---------- Reports ----------
    void printStock() {
        System.out.println("\n--- Blood Stock Report ---");
        System.out.printf("%-12s %-10s %-10s %-12s%n", "Blood Group", "Units", "Status", "Last Updated");
        for (BloodStock s : db.stock.values())
            System.out.printf("%-12s %-10d %-10s %-12s%n",
                    s.getBloodGroup(), s.getAvailableUnits(), s.getStatus(), s.getLastUpdated());
    }

    void printDonors() {
        System.out.println("\n--- Donor List ---");
        if (db.donors.isEmpty()) { System.out.println("No donors registered."); return; }
        System.out.printf("%-8s %-20s %-5s %-12s %-6s%n", "ID", "Name", "Age", "Contact", "Group");
        for (Donor d : db.donors.values())
            System.out.printf("%-8s %-20s %-5d %-12s %-6s%n",
                    d.getDonorId(), d.getName(), d.getAge(), d.getContact(), d.getBloodGroup());
    }

    void printDonations() {
        System.out.println("\n--- Donation History ---");
        if (db.donations.isEmpty()) { System.out.println("No donations recorded."); return; }
        System.out.printf("%-8s %-8s %-12s %-6s %-6s %-10s%n", "ID", "Donor", "Date", "Group", "Units", "Status");
        for (Donation d : db.donations.values())
            System.out.printf("%-8s %-8s %-12s %-6s %-6d %-10s%n", d.getDonationId(), d.getDonorId(),
                    d.getDonationDate(), d.getBloodGroup(), d.getUnits(), d.getStatus());
    }

    void printRequests() {
        System.out.println("\n--- Blood Requests ---");
        if (db.requests.isEmpty()) { System.out.println("No requests recorded."); return; }
        System.out.printf("%-8s %-20s %-6s %-6s %-10s%n", "ID", "Requester", "Group", "Units", "Status");
        for (BloodRequest r : db.requests.values())
            System.out.printf("%-8s %-20s %-6s %-6d %-10s%n", r.getRequestId(), r.getRequester(),
                    r.getBloodGroup(), r.getUnitsRequired(), r.getStatus());
    }

    void printIssues() {
        System.out.println("\n--- Issue Transactions ---");
        if (db.issues.isEmpty()) { System.out.println("No blood issued yet."); return; }
        System.out.printf("%-8s %-8s %-6s %-12s %-12s%n", "Issue", "Request", "Units", "Date", "Approved By");
        for (BloodIssue i : db.issues.values())
            System.out.printf("%-8s %-8s %-6d %-12s %-12s%n", i.getIssueId(), i.getRequestId(),
                    i.getUnitsIssued(), i.getIssueDate(), i.getApprovedBy());
    }
}

// ===================== User Interface =====================

public class BloodBankManagementSystem {
    private static final Scanner sc = new Scanner(System.in);
    private static BloodBankService service;

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.hasNextLine() ? sc.nextLine().trim() : "";
    }

    private static int readInt(String prompt) throws BloodBankException {
        String s = readLine(prompt);
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { throw new BloodBankException("Please enter a valid number."); }
    }

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  BLOOD DONATION & BLOOD BANK MANAGEMENT  ");
        System.out.println("==========================================");
        service = new BloodBankService(new DataStore());

        User user = null;
        for (int attempt = 1; attempt <= 3 && user == null; attempt++) {
            String u = readLine("Username: ");
            String p = readLine("Password: ");
            user = service.login(u, p);
            if (user == null) System.out.println("Invalid login (" + attempt + "/3).");
        }
        if (user == null) { System.out.println("Too many failed attempts. Exiting."); return; }
        System.out.println("Welcome, " + user.getUsername() + " (" + user.getRole() + ")");

        boolean running = true;
        while (running) {
            showMenu(user.getRole());
            String choice = readLine("Enter choice: ");
            try {
                running = handle(choice, user);
            } catch (BloodBankException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
        System.out.println("Thank you. Goodbye!");
    }

    private static void showMenu(Role role) {
        System.out.println("\n========== MAIN MENU ==========");
        if (role != Role.REQUESTER) {
            System.out.println(" 1. Register Donor");
            System.out.println(" 2. Record Donation");
            System.out.println(" 3. Process Blood Request (approve & issue)");
            System.out.println(" 4. Reject Blood Request");
        }
        System.out.println(" 5. Submit Blood Request");
        System.out.println(" 6. View Blood Stock");
        if (role != Role.REQUESTER) {
            System.out.println(" 7. View Donors");
            System.out.println(" 8. View Donation History");
            System.out.println(" 9. View Issue Transactions");
        }
        System.out.println("10. View Blood Requests");
        if (role == Role.ADMIN) System.out.println("11. Add User");
        System.out.println(" 0. Logout / Exit");
    }

    private static boolean handle(String choice, User user) throws BloodBankException {
        boolean staff = user.getRole() != Role.REQUESTER;
        switch (choice) {
            case "1":
                requireStaff(staff);
                String name = readLine("Donor name: ");
                int age = readInt("Age: ");
                String contact = readLine("Contact (10 digits): ");
                String group = readLine("Blood group: ");
                Donor d = service.registerDonor(name, age, contact, group);
                System.out.println("Donor registered. ID = " + d.getDonorId());
                break;
            case "2":
                requireStaff(staff);
                String did = readLine("Donor ID: ");
                int units = readInt("Units collected (1-5): ");
                Donation don = service.recordDonation(did, units);
                System.out.println("Donation recorded. ID = " + don.getDonationId()
                        + ", stock updated for " + don.getBloodGroup());
                break;
            case "3":
                requireStaff(staff);
                service.printRequests();
                String rid = readLine("Request ID to approve: ");
                String confirm = readLine("Confirm medical approval obtained? (y/n): ");
                if (!confirm.equalsIgnoreCase("y")) {
                    System.out.println("Issue cancelled. Approval by qualified staff is required.");
                    break;
                }
                BloodIssue issue = service.approveAndIssue(rid, user.getUsername());
                System.out.println("Blood issued. Issue ID = " + issue.getIssueId()
                        + ", units = " + issue.getUnitsIssued());
                service.printStock();
                break;
            case "4":
                requireStaff(staff);
                service.rejectRequest(readLine("Request ID to reject: "));
                System.out.println("Request rejected.");
                break;
            case "5":
                String requester = readLine("Patient / Hospital name: ");
                String g = readLine("Blood group required: ");
                int q = readInt("Units required: ");
                BloodRequest r = service.createRequest(requester, g, q);
                System.out.println("Request submitted. ID = " + r.getRequestId() + " (PENDING)");
                break;
            case "6": service.printStock(); break;
            case "7": requireStaff(staff); service.printDonors(); break;
            case "8": requireStaff(staff); service.printDonations(); break;
            case "9": requireStaff(staff); service.printIssues(); break;
            case "10": service.printRequests(); break;
            case "11":
                if (user.getRole() != Role.ADMIN) throw new BloodBankException("Access denied. Admin only.");
                String un = readLine("New username: ");
                String pw = readLine("Password: ");
                String rl = readLine("Role (ADMIN/OPERATOR/REQUESTER): ").toUpperCase();
                Role role;
                try { role = Role.valueOf(rl); }
                catch (IllegalArgumentException e) { throw new BloodBankException("Invalid role."); }
                User nu = service.addUser(un, pw, role);
                System.out.println("User created. ID = " + nu.getUserId());
                break;
            case "0": return false;
            default: System.out.println("Invalid choice. Try again.");
        }
        return true;
    }

    private static void requireStaff(boolean staff) throws BloodBankException {
        if (!staff) throw new BloodBankException("Access denied. Authorized staff only.");
    }
}
