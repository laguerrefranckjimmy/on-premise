// reportWebVitals.js
import { getCLS, getFID, getFCP, getLCP, getTTFB } from 'web-vitals';

/**
 * Report Web Vitals to console, monitoring system, or analytics endpoint.
 * @param {function|undefined} onPerfEntry - callback to handle metrics
 */
const reportWebVitals = (onPerfEntry) => {
    if (onPerfEntry && typeof onPerfEntry === 'function') {
        // Collect key Web Vitals metrics
        getCLS(onPerfEntry);
        getFID(onPerfEntry);
        getFCP(onPerfEntry);
        getLCP(onPerfEntry);
        getTTFB(onPerfEntry);
    }
};

export default reportWebVitals;

