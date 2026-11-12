const navItems = [
    ["dashboard.html", "Dashboard", "⌂"],
    ["create-account.html", "Create account", "+"],
    ["deposit.html", "Deposit", "＋"],
    ["withdraw.html", "Withdraw", "↗"],
    ["balance.html", "Check balance", "◎"],
    ["account-details.html", "Account details", "◉"],
    ["transactions.html", "Transaction history", "≡"]
];

const currency = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });

async function requestJson(url, options = {}) {
    let response;
    try {
        response = await fetch(url, { credentials: "same-origin", cache: "no-store", ...options });
    } catch {
        throw new Error("Cannot reach the Java backend. Start Tomcat and open the deployed app over HTTP (for example, http://localhost:8080/bank-management/); do not open the HTML file directly.");
    }
    let data;
    try {
        data = await response.json();
    } catch {
        throw new Error("The server returned an unexpected response. Confirm the application is deployed on Tomcat and the URL includes /bank-management/.");
    }
    if (response.status === 401) {
        window.location.assign("login.html");
        throw new Error("Please sign in to continue.");
    }
    if (!response.ok) throw new Error(data.error || "Something went wrong. Please try again.");
    return data;
}

function formData(form) {
    return new URLSearchParams(new FormData(form));
}

function showMessage(form, message, success = false) {
    const target = form.querySelector(".form-message");
    if (!target) return;
    target.textContent = message;
    target.classList.toggle("success", success);
}

function money(value) {
    return currency.format(Number(value || 0));
}

function displayDate(value, includeTime = false) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat("en-IN", includeTime
        ? { dateStyle: "medium", timeStyle: "short" }
        : { dateStyle: "medium" }).format(date);
}

function renderTransactions(rows, limit = Infinity) {
    const target = document.querySelector("[data-transactions]");
    if (!target) return;
    target.replaceChildren();
    const list = rows.slice(0, limit);
    if (!list.length) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = document.querySelector("thead tr")?.children.length || 4;
        cell.className = "empty-state";
        cell.textContent = "No transactions yet. Your account activity will appear here.";
        row.append(cell);
        target.append(row);
        return;
    }
    list.forEach((transaction) => {
        const row = document.createElement("tr");
        const typeCell = document.createElement("td");
        const type = document.createElement("span");
        type.className = "transaction-type";
        const dot = document.createElement("span");
        dot.className = `type-dot${transaction.transactionType === "WITHDRAWAL" ? " withdrawal" : ""}`;
        type.append(dot, document.createTextNode(transaction.transactionType));
        typeCell.append(type);
        row.append(typeCell);

        if (document.querySelector("thead tr")?.children.length === 5) {
            const reference = document.createElement("td");
            reference.textContent = `#${transaction.transactionId}`;
            row.append(reference);
        }

        const dateCell = document.createElement("td");
        dateCell.textContent = displayDate(transaction.transactionDate, true);
        row.append(dateCell);
        const amountCell = document.createElement("td");
        const isDebit = transaction.transactionType === "WITHDRAWAL";
        amountCell.className = isDebit ? "amount-negative" : "amount-positive";
        amountCell.textContent = `${isDebit ? "−" : "+"}${money(transaction.amount)}`;
        row.append(amountCell);
        const balanceCell = document.createElement("td");
        balanceCell.textContent = money(transaction.balanceAfterTransaction);
        row.append(balanceCell);
        target.append(row);
    });
}

function renderAccount(account) {
    document.querySelectorAll("[data-user-name]").forEach((el) => { el.textContent = account.fullName; });
    document.querySelectorAll("[data-first-name]").forEach((el) => {
        el.textContent = account.fullName.trim().split(/\s+/)[0];
    });
    document.querySelectorAll("[data-initials]").forEach((el) => {
        el.textContent = account.fullName.trim().charAt(0).toUpperCase();
    });
    document.querySelectorAll("[data-account-number]").forEach((el) => { el.textContent = account.accountNumber; });
    document.querySelectorAll("[data-balance]").forEach((el) => { el.textContent = money(account.balance); });
    document.querySelectorAll("[data-detail]").forEach((el) => {
        const key = el.dataset.detail;
        let value = account[key];
        if (key === "balance") value = money(value);
        if (key === "dob" || key === "createdAt") value = displayDate(value);
        if (key === "gender" || key === "accountType") value = String(value || "").toLowerCase().replace(/^./, c => c.toUpperCase());
        el.textContent = value || "—";
    });
}

function renderNavigation() {
    const nav = document.getElementById("app-nav");
    if (!nav) return;
    const current = window.location.pathname.split("/").pop();
    const label = document.createElement("p");
    label.className = "nav-label";
    label.textContent = "YOUR BANKING";
    const list = document.createElement("nav");
    list.className = "nav-list";
    navItems.forEach(([href, title, icon]) => {
        const link = document.createElement("a");
        link.className = `nav-link${current === href ? " active" : ""}`;
        link.href = href;
        if (current === href) link.setAttribute("aria-current", "page");
        const glyph = document.createElement("span");
        glyph.className = "nav-icon";
        glyph.setAttribute("aria-hidden", "true");
        glyph.textContent = icon;
        link.append(glyph, document.createTextNode(title));
        list.append(link);
    });
    nav.append(label, list);
}

async function initialiseProtectedPage() {
    if (!document.getElementById("app-nav") && !document.querySelector("[data-user-name]")) return null;
    const session = await requestJson("api/session");
    renderAccount(session.account);
    return session;
}

function wireLogout(sessionPromise) {
    document.querySelectorAll("[data-logout]").forEach((button) => {
        button.addEventListener("click", async () => {
            button.disabled = true;
            try {
                const session = await sessionPromise;
                if (!session) throw new Error("Your account session could not be loaded. Refresh and try again.");
                await requestJson("api/logout", {
                    method: "POST",
                    headers: { "X-CSRF-Token": session.csrfToken }
                });
                window.location.assign("login.html");
            } catch (error) {
                button.disabled = false;
                window.alert(error.message);
            }
        });
    });
}

function wireForms(sessionPromise) {
    document.querySelectorAll("form[data-form]").forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!form.reportValidity()) return;
            const button = form.querySelector('[type="submit"]');
            button.disabled = true;
            showMessage(form, "");
            try {
                const type = form.dataset.form;
                if (type === "login") {
                    const result = await requestJson("api/login", { method: "POST", body: formData(form) });
                    window.location.assign(result.redirect);
                    return;
                }
                const url = type === "create-account" ? "api/accounts" : `api/${type}`;
                const options = { method: "POST", body: formData(form) };
                if (type !== "create-account") {
                    const session = await sessionPromise;
                    if (!session) throw new Error("Your account session could not be loaded. Refresh and try again.");
                    options.headers = { "X-CSRF-Token": session.csrfToken };
                }
                const result = await requestJson(url, options);
                if (type === "create-account") {
                    showMessage(form, `${result.message} Your account number is ${result.accountNumber}. Save it for sign-in.`, true);
                    form.reset();
                    return;
                }
                showMessage(form, `${result.message} New balance: ${money(result.transaction.balanceAfterTransaction)}.`, true);
                form.reset();
                if (type === "withdraw") {
                    document.querySelectorAll("[data-balance]").forEach((el) => {
                        el.textContent = money(result.transaction.balanceAfterTransaction);
                    });
                }
            } catch (error) {
                showMessage(form, error.message);
            } finally {
                button.disabled = false;
            }
        });
    });
}

async function loadPageData() {
    const path = window.location.pathname.split("/").pop();
    if (path === "dashboard.html") {
        const data = await requestJson("api/transactions");
        renderTransactions(data.transactions, 5);
    } else if (path === "transactions.html") {
        const data = await requestJson("api/transactions");
        renderTransactions(data.transactions);
    } else if (path === "balance.html") {
        const data = await requestJson("api/balance");
        document.querySelectorAll("[data-balance]").forEach((el) => { el.textContent = money(data.balance); });
        document.querySelectorAll("[data-account-number]").forEach((el) => { el.textContent = data.accountNumber; });
    }
}

document.addEventListener("DOMContentLoaded", () => {
    renderNavigation();
    const sessionPromise = initialiseProtectedPage().catch((error) => {
        const main = document.querySelector(".app-main");
        if (main) {
            const notice = document.createElement("p");
            notice.className = "page-error";
            notice.setAttribute("role", "alert");
            notice.textContent = error.message;
            main.prepend(notice);
        }
        return null;
    });
    wireLogout(sessionPromise);
    wireForms(sessionPromise);
    loadPageData().catch((error) => {
        const target = document.querySelector("[data-transactions]");
        if (target) {
            target.replaceChildren();
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 5;
            cell.className = "empty-state";
            cell.textContent = error.message;
            row.append(cell);
            target.append(row);
        }
    });
});
