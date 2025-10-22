import React from 'react';
import { BrowserRouter as Router, Routes, Route, NavLink } from 'react-router-dom';
import InventoryDashboard from './InventoryDashboard';
import NewOrderPage from './NewOrderPage';

function App() {
    return (
        <Router>
            <div className="min-h-screen bg-gray-100">
                {/* Header */}
                <header className="bg-blue-600 text-white p-4 shadow-md">
                    <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between">
                        <h1 className="text-2xl font-bold">On-Premise Business</h1>
                        <p className="mt-1 md:mt-0 text-sm md:text-base">Welcome to my Home Page</p>
                    </div>
                </header>

                {/* Navigation */}
                <nav className="bg-white shadow-sm">
                    <div className="max-w-6xl mx-auto flex space-x-4 p-3">
                        <NavLink
                            to="/"
                            end
                            className={({ isActive }) =>
                                isActive
                                    ? 'text-blue-600 font-semibold border-b-2 border-blue-600'
                                    : 'text-gray-700 hover:text-blue-600'
                            }
                        >
                            Inventory Dashboard
                        </NavLink>
                        <NavLink
                            to="/new-order"
                            className={({ isActive }) =>
                                isActive
                                    ? 'text-blue-600 font-semibold border-b-2 border-blue-600'
                                    : 'text-gray-700 hover:text-blue-600'
                            }
                        >
                            New Order
                        </NavLink>
                    </div>
                </nav>

                {/* Main Content */}
                <main className="max-w-6xl mx-auto p-6">
                    <Routes>
                        <Route path="/" element={<InventoryDashboard />} />
                        <Route path="/new-order" element={<NewOrderPage />} />
                    </Routes>
                </main>
            </div>
        </Router>
    );
}

export default App;
