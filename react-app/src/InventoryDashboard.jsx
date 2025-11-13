import React, { useEffect, useState } from 'react';

function InventoryDashboard() {
  const [inventory, setInventory] = useState({});
  const [recentOrders, setRecentOrders] = useState([]);
  const [highlightedInventory, setHighlightedInventory] = useState([]);
  const [highlightedOrders, setHighlightedOrders] = useState([]);
  const [wsStatus, setWsStatus] = useState('connecting');

  useEffect(() => {
    // Must match Vert.x: port 8081, path /ws
    const ws = new WebSocket('ws://127.0.0.1:8081/ws');

    ws.onopen = () => {
      console.log('WebSocket connected');
      setWsStatus('connected');
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);

        if (data.type === 'inventory.updated') {
          // update inventory map
          setInventory((prev) => ({
            ...prev,
            [data.productId]: data.quantity,
          }));

          // highlight product briefly
          setHighlightedInventory((prev) => [...prev, data.productId]);
          setTimeout(() => {
            setHighlightedInventory((prev) =>
              prev.filter((id) => id !== data.productId)
            );
          }, 1500);
        }

        if (data.type === 'order.created') {
          // prepend latest, keep last 10
          setRecentOrders((prev) => [data, ...prev.slice(0, 9)]);

          // highlight order briefly
          setHighlightedOrders((prev) => [...prev, data.orderId]);
          setTimeout(() => {
            setHighlightedOrders((prev) =>
              prev.filter((id) => id !== data.orderId)
            );
          }, 1500);
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setWsStatus('error');
    };

    ws.onclose = () => {
      console.log('WebSocket disconnected');
      setWsStatus('disconnected');
    };

    return () => ws.close();
  }, []);

  return (
    <div className="p-6 bg-gray-100 min-h-screen">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Inventory Dashboard</h1>
        <span
          className={`px-3 py-1 rounded-full text-xs font-semibold
            ${
              wsStatus === 'connected'
                ? 'bg-green-100 text-green-700'
                : wsStatus === 'connecting'
                ? 'bg-yellow-100 text-yellow-700'
                : 'bg-red-100 text-red-700'
            }`}
        >
          WebSocket: {wsStatus}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Inventory Section */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-4">Inventory</h2>
          {Object.keys(inventory).length === 0 ? (
            <div className="text-gray-500">No inventory data</div>
          ) : (
            <div className="space-y-2">
              {Object.entries(inventory).map(([product, qty]) => {
                const isHighlighted = highlightedInventory.includes(product);
                return (
                  <div
                    key={product}
                    className={`flex justify-between p-2 rounded transition
                      ${
                        isHighlighted
                          ? 'bg-green-100 animate-pulse'
                          : 'bg-gray-50 hover:bg-gray-100'
                      }`}
                  >
                    <span className="font-medium">{product}</span>
                    <span className="font-bold">{qty}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Recent Orders Section */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-4">Recent Orders</h2>
          {recentOrders.length === 0 ? (
            <div className="text-gray-500">No orders yet</div>
          ) : (
            <ul className="space-y-2">
              {recentOrders.map((order) => {
                const isHighlighted = highlightedOrders.includes(order.orderId);
                return (
                  <li
                    key={order.orderId}
                    className={`p-2 rounded flex justify-between transition
                      ${
                        isHighlighted
                          ? 'bg-green-100 animate-pulse'
                          : 'bg-gray-50 hover:bg-gray-100'
                      }`}
                  >
                    <span>
                      <span className="font-medium">
                        {order.productId}
                      </span>{' '}
                      - Qty: {order.quantity}
                    </span>
                    <span className="text-gray-400 text-sm">
                      ID: {order.orderId}
                    </span>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

export default InventoryDashboard;
