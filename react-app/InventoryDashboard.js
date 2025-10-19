import React, { useEffect, useState } from 'react';

function InventoryDashboard() {
  const [inventory, setInventory] = useState({});

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
    };
    return () => ws.close();
  }, []);

  return (
    <div>
      <h1>Inventory</h1>
      {Object.entries(inventory).map(([product, qty]) => (
        <div key={product}>{product}: {qty}</div>
      ))}
    </div>
  );
}

export default InventoryDashboard;
