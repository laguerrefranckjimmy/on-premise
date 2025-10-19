import React, { useEffect, useState } from 'react';

function InventoryDashboard() {
  const [inventory, setInventory] = useState({});
  const [recentOrders, setRecentOrders] = useState([]);

  useEffect(() => {
    const ws = new WebSocket('ws://localhost:8081');
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);

      if(data.type === 'inventory.updated'){
        setInventory(prev => ({
          ...prev,
          [data.productId]: data.quantity
        }));
      }

      if(data.type === 'order.created'){
        setRecentOrders(prev => [data, ...prev.slice(0, 9)]); // keep last 10 orders
      }
    });

    return () => ws.close();
  }, []);

  return (
    <div style={{ display: 'flex', gap: '2rem' }}>
      {/* Inventory Section */}
      <div>
        <h2>Inventory</h2>
        {Object.entries(inventory).map(([product, qty]) => (
          <div key={product}>{product}: {qty}</div>
        ))}
      </div>

      {/* Recent Orders Section */}
      <div>
        <h2>Recent Orders</h2>
        {recentOrders.length === 0 ? (
          <div>No orders yet</div>
        ) : (
          <ul>
            {recentOrders.map(order => (
              <li key={order.orderId}>
                {order.productId} - Qty: {order.quantity} - Order ID: {order.orderId}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default InventoryDashboard;
