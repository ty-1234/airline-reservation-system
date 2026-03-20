const STORAGE_KEY = "skyledger-session";
const FLASH_KEY = "skyledger-flash";

const state = {
    token: null,
    user: null
};

const messageBox = document.getElementById("message-box");
const sessionStatus = document.getElementById("session-status");
const signOutButton = document.getElementById("sign-out");

loadSession();
renderSession();
renderQueuedFlash();
bindCommonActions();
bindHomeSearch();
bindAuthForms();
bindSearchPage();
bindBookingPage();
hydrateSearchFromQuery();
hydrateBookingFromQuery();

function bindCommonActions() {
    if (!signOutButton) {
        return;
    }

    signOutButton.addEventListener("click", () => {
        clearSession();
        queueFlash("Signed out successfully.", false);
        window.location.href = "/";
    });
}

function bindHomeSearch() {
    const form = document.getElementById("home-search-form");
    if (!form) {
        return;
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        window.location.href = `/search.html?${new URLSearchParams(formData(form)).toString()}`;
    });
}

function bindAuthForms() {
    const registerForm = document.getElementById("register-form");
    const loginForm = document.getElementById("login-form");

    if (registerForm) {
        registerForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = formData(registerForm);
            const response = await api("/api/auth/register", "POST", payload);
            if (!response) {
                return;
            }

            setSession(response);
            queueFlash(`Welcome aboard, ${response.fullName}.`, false);
            window.location.href = "/search.html";
        });
    }

    if (loginForm) {
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = formData(loginForm);
            const response = await api("/api/auth/login", "POST", payload);
            if (!response) {
                return;
            }

            setSession(response);
            queueFlash(`Signed in as ${response.fullName}.`, false);
            window.location.href = "/search.html";
        });
    }
}

function bindSearchPage() {
    const searchForm = document.getElementById("search-form");
    const seatForm = document.getElementById("seat-form");

    if (searchForm) {
        searchForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await runFlightSearch(searchForm);
        });
    }

    if (seatForm) {
        seatForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const seatResults = document.getElementById("seat-results");
            const { flightId } = formData(seatForm);
            const seats = await api(`/api/flights/${flightId}/seats`);
            if (!seats) {
                return;
            }

            if (seats.length === 0) {
                renderEmptyState(seatResults, "No seats returned", "This flight does not have published inventory yet.");
                return;
            }

            seatResults.innerHTML = seats.map((seat) => `
                <article class="seat-card">
                    <div class="seat-topline">
                        <strong>${seat.seatNumber}</strong>
                        <span class="status-pill ${seat.status.toLowerCase()}">${seat.status}</span>
                    </div>
                    <div>${seat.seatClass}</div>
                    <p>${formatMoney(seat.price)}</p>
                </article>
            `).join("");

            flash(`Loaded ${seats.length} seat record${seats.length === 1 ? "" : "s"} for flight ${flightId}.`);
        });
    }
}

function bindBookingPage() {
    const holdForm = document.getElementById("hold-form");
    const confirmForm = document.getElementById("confirm-form");
    const lookupForm = document.getElementById("lookup-form");
    const cancelForm = document.getElementById("cancel-form");
    const myBookingsButton = document.getElementById("my-bookings");

    if (holdForm) {
        holdForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!requireSession()) {
                return;
            }

            const data = formData(holdForm);
            const payload = {
                flightId: Number(data.flightId),
                seatNumbers: [data.seatNumber],
                passengers: [{
                    fullName: data.fullName,
                    age: Number(data.age),
                    gender: data.gender,
                    passportNumber: data.passportNumber
                }]
            };

            const booking = await api("/api/bookings/hold", "POST", payload, true);
            if (!booking) {
                return;
            }

            renderBookings([booking]);
            const pnrField = document.querySelector("#confirm-form [name='pnr']");
            if (pnrField) {
                pnrField.value = booking.pnr;
            }
            flash(`Hold created. PNR ${booking.pnr} remains active until ${formatDate(booking.holdExpiresAt)}.`);
            holdForm.reset();
        });
    }

    if (confirmForm) {
        confirmForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!requireSession()) {
                return;
            }

            const data = formData(confirmForm);
            const booking = await api(`/api/bookings/${data.pnr}/confirm`, "POST", {
                paymentMethod: data.paymentMethod,
                paymentReference: data.paymentReference
            }, true);

            if (!booking) {
                return;
            }

            renderBookings([booking]);
            flash(`Booking ${booking.pnr} confirmed successfully.`);
            confirmForm.reset();
        });
    }

    if (lookupForm) {
        lookupForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!requireSession()) {
                return;
            }

            const { pnr } = formData(lookupForm);
            const booking = await api(`/api/bookings/${pnr}`, "GET", null, true);
            if (!booking) {
                return;
            }

            renderBookings([booking]);
            flash(`Booking ${booking.pnr} loaded.`);
        });
    }

    if (cancelForm) {
        cancelForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!requireSession()) {
                return;
            }

            const { pnr } = formData(cancelForm);
            const response = await api(`/api/bookings/${pnr}/cancel`, "POST", null, true);
            if (!response) {
                return;
            }

            flash(`Booking ${response.pnr} cancelled. Refund amount: ${formatMoney(response.refundAmount)}.`);
            cancelForm.reset();
        });
    }

    if (myBookingsButton) {
        myBookingsButton.addEventListener("click", async () => {
            if (!requireSession()) {
                return;
            }

            const bookings = await api(`/api/users/${state.user.userId}/bookings`, "GET", null, true);
            if (!bookings) {
                return;
            }

            renderBookings(bookings);
            flash(`Loaded ${bookings.length} booking record${bookings.length === 1 ? "" : "s"}.`);
        });
    }
}

async function runFlightSearch(form) {
    const searchResults = document.getElementById("search-results");
    const query = new URLSearchParams(formData(form));
    history.replaceState({}, "", `/search.html?${query.toString()}`);

    const flights = await api(`/api/flights/search?${query.toString()}`);
    if (!flights) {
        return;
    }

    if (flights.length === 0) {
        renderEmptyState(searchResults, "No flights available", "Try a different route, date, or cabin class.");
        return;
    }

    searchResults.innerHTML = flights.map((flight) => `
        <article class="result-card">
            <div class="result-topline">
                <span class="route-pill">${flight.sourceCode} to ${flight.destinationCode}</span>
                <span class="fare-tag">${formatMoney(flight.startingFare)}</span>
            </div>
            <h3>${flight.flightNumber}</h3>
            <div class="result-meta">
                <span>${flight.sourceCity} to ${flight.destinationCity}</span>
                <span>Departure: ${formatDate(flight.departureTime)}</span>
                <span>Available seats: ${flight.availableSeats}</span>
            </div>
            <div class="result-footer">
                <button type="button" class="button button-ghost inspect-flight" data-flight-id="${flight.id}">Inspect seats</button>
                <a href="/bookings.html?flightId=${flight.id}" class="button button-primary">Continue</a>
            </div>
        </article>
    `).join("");

    searchResults.querySelectorAll(".inspect-flight").forEach((button) => {
        button.addEventListener("click", () => {
            const flightId = button.dataset.flightId;
            const seatForm = document.getElementById("seat-form");
            if (!seatForm) {
                return;
            }

            seatForm.elements.flightId.value = flightId;
            seatForm.requestSubmit();
        });
    });

    flash(`Found ${flights.length} matching flight option${flights.length === 1 ? "" : "s"}.`);
}

function hydrateSearchFromQuery() {
    const searchForm = document.getElementById("search-form");
    if (!searchForm) {
        return;
    }

    const params = new URLSearchParams(window.location.search);
    let hasSearchInput = false;

    ["source", "destination", "date", "seatClass", "passengers"].forEach((key) => {
        const value = params.get(key);
        if (!value || !searchForm.elements[key]) {
            return;
        }

        searchForm.elements[key].value = value;
        hasSearchInput = true;
    });

    if (hasSearchInput && searchForm.elements.source.value && searchForm.elements.destination.value && searchForm.elements.date.value) {
        runFlightSearch(searchForm);
    }
}

function hydrateBookingFromQuery() {
    const holdForm = document.getElementById("hold-form");
    if (!holdForm) {
        return;
    }

    const flightId = new URLSearchParams(window.location.search).get("flightId");
    if (flightId) {
        holdForm.elements.flightId.value = flightId;
        flash(`Flight ${flightId} carried into the booking form.`);
    }
}

function renderBookings(bookings) {
    const bookingResults = document.getElementById("booking-results");
    if (!bookingResults) {
        return;
    }

    if (bookings.length === 0) {
        renderEmptyState(bookingResults, "No bookings found", "This traveler does not have any bookings yet.");
        return;
    }

    bookingResults.innerHTML = bookings.map((booking) => `
        <article class="result-card">
            <div class="result-topline">
                <span class="route-pill">${booking.pnr}</span>
                <span class="status-pill ${booking.status.toLowerCase()}">${booking.status}</span>
            </div>
            <h3>${booking.flightNumber}</h3>
            <div class="result-meta">
                <span>${booking.route}</span>
                <span>Departure: ${formatDate(booking.departureTime)}</span>
                <span>Total fare: ${formatMoney(booking.totalFare)}</span>
                <span>Seats: ${booking.seatNumbers.join(", ")}</span>
            </div>
        </article>
    `).join("");
}

function renderEmptyState(container, title, copy) {
    if (!container) {
        return;
    }

    container.innerHTML = `
        <article class="empty-state">
            <h3>${title}</h3>
            <p>${copy}</p>
        </article>
    `;
}

function requireSession() {
    if (state.token) {
        return true;
    }

    queueFlash("Please sign in before using protected booking actions.", true);
    window.location.href = "/auth.html";
    return false;
}

async function api(url, method = "GET", body = null, auth = false) {
    try {
        const response = await fetch(url, {
            method,
            headers: {
                "Content-Type": "application/json",
                ...(auth && state.token ? { Authorization: `Bearer ${state.token}` } : {})
            },
            body: body ? JSON.stringify(body) : null
        });

        const contentType = response.headers.get("content-type") || "";
        const payload = contentType.includes("application/json") ? await response.json() : await response.text();

        if (!response.ok) {
            const message = typeof payload === "string" ? payload : payload.message || "Request failed.";
            flash(message, true);
            return null;
        }

        return payload;
    } catch (error) {
        flash(error.message || "Unexpected request failure.", true);
        return null;
    }
}

function setSession(authResponse) {
    state.token = authResponse.token;
    state.user = authResponse;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(authResponse));
    renderSession();
}

function loadSession() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
        return;
    }

    try {
        const session = JSON.parse(raw);
        state.token = session.token;
        state.user = session;
    } catch (error) {
        clearSession();
    }
}

function clearSession() {
    state.token = null;
    state.user = null;
    localStorage.removeItem(STORAGE_KEY);
    renderSession();
}

function renderSession() {
    if (!sessionStatus) {
        return;
    }

    if (state.user) {
        sessionStatus.textContent = `${state.user.fullName} | ${state.user.role}`;
        sessionStatus.classList.add("is-live");
        if (signOutButton) {
            signOutButton.hidden = false;
        }
        return;
    }

    sessionStatus.textContent = "Guest mode";
    sessionStatus.classList.remove("is-live");
    if (signOutButton) {
        signOutButton.hidden = true;
    }
}

function flash(message, isError = false) {
    if (!messageBox) {
        return;
    }

    messageBox.textContent = message;
    messageBox.classList.toggle("is-error", isError);
    messageBox.classList.toggle("is-success", !isError);
}

function queueFlash(message, isError) {
    sessionStorage.setItem(FLASH_KEY, JSON.stringify({ message, isError }));
}

function renderQueuedFlash() {
    const raw = sessionStorage.getItem(FLASH_KEY);
    if (!raw) {
        return;
    }

    sessionStorage.removeItem(FLASH_KEY);

    try {
        const { message, isError } = JSON.parse(raw);
        flash(message, Boolean(isError));
    } catch (error) {
        flash("Ready.");
    }
}

function formData(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function formatDate(value) {
    if (!value) {
        return "N/A";
    }

    return new Date(value).toLocaleString();
}

function formatMoney(value) {
    const amount = Number(value);
    if (Number.isNaN(amount)) {
        return value ?? "N/A";
    }

    return new Intl.NumberFormat(undefined, {
        style: "currency",
        currency: "USD",
        maximumFractionDigits: 2
    }).format(amount);
}
