# EDRS Web UI

A single-page AngularJS application with Bootstrap for the Equipment & Device Reservation System.

## Features

- **Login Page**: Stub authentication that accepts any username and stores it
- **Make Reservation**: Create reservations with inventory items and date selection
- **List Inventory**: View all available inventory items
- **My Reservations**: View and cancel your own reservations
- **Admin Features**:
  - View all reservations from all users
  - Add new inventory items

## Requirements

- Modern web browser (Chrome, Firefox, Edge, Safari)
- Backend services running:
  - Reservation Service on `http://localhost:8080`
  - Inventory Service on `http://localhost:8081`

## Usage

### Option 1: Docker (Recommended)

The easiest way to run the UI is using Docker Compose with the rest of the EDRS services:

```bash
# From the project root
docker-compose up -d web-ui
```

Or start all services including the UI:

```bash
docker-compose up -d
```

Then open `http://localhost:8000` in your browser.

The UI will automatically connect to the backend services running in Docker.

### Option 2: Using a Simple HTTP Server

#### Python 3
```bash
cd web-ui
python -m http.server 8000
```
Then open `http://localhost:8000` in your browser.

#### Node.js (http-server)
```bash
cd web-ui
npx http-server -p 8000
```
Then open `http://localhost:8000` in your browser.

#### PHP
```bash
cd web-ui
php -S localhost:8000
```
Then open `http://localhost:8000` in your browser.

### Option 3: Open Directly in Browser

Simply open `index.html` in your web browser. Note that due to CORS restrictions, you may need to serve the file through a web server.

## Configuration

The API endpoints are configured in the `API_CONFIG` constant in `index.html`:

```javascript
app.constant('API_CONFIG', {
    reservationService: 'http://localhost:8080',
    inventoryService: 'http://localhost:8081'
});
```

If your services run on different ports or hosts, update these values.

## Admin Access

Users with usernames starting with "admin" (case-insensitive) are automatically granted admin privileges. For example:
- `admin`
- `admin1`
- `administrator`

Admin users can:
- View all reservations from all users
- Add new inventory items

## Features in Detail

### Login
- Enter any username to log in
- Username is stored in browser localStorage
- Admin users (usernames starting with "admin") get additional privileges

### Make Reservation
- Select a reservation date and time using the date picker
- Choose inventory items from a dropdown
- Specify quantities for each selected item
- Submit to create a reservation

### My Reservations
- View all your reservations
- See confirmation numbers, dates, items, and status
- Cancel pending or confirmed reservations

### Inventory List
- View all available inventory items
- See ID, name, description, category, and available quantity

### Admin: All Reservations
- View reservations from all users
- See user information for each reservation

### Admin: Add Inventory
- Create new inventory items
- Specify ID, name, description, category, and initial quantity

## Notes

- The application uses AngularJS 1.8.3 and Bootstrap 5.3.0 via CDN
- All templates are embedded inline using `$templateCache`
- Authentication is stubbed - any username will work
- The UI is responsive and works on mobile devices
- Date/time pickers use the browser's native datetime-local input

## Troubleshooting

### CORS Errors
If you see CORS errors when making API calls, ensure:
1. The backend services are running
2. The services allow CORS from your UI origin
3. You're accessing the UI through `http://localhost` or `http://127.0.0.1` (not `file://`)

### API Connection Issues
- Verify the services are running on the configured ports
- Check browser console for detailed error messages
- Ensure the API endpoints match the configuration in `index.html`
