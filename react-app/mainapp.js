import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import InventoryDashboard from './InventoryDashboard';
import NewOrderPage from './NewOrderPage';

function App() {
  return (
    <Router>
      <div>
        <nav>
          <Link to="/">Inventory Dashboard</Link> | <Link to="/new-order">New Order</Link>
        </nav>
        <Routes>
          <Route path="/" element={<InventoryDashboard />} />
          <Route path="/new-order" element={<NewOrderPage />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
