import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const vus = Number(__ENV.VUS || "10");
const warmup = __ENV.WARMUP || "30s";
const duration = __ENV.DURATION || "5m";

export const options = {
  scenarios: {
    orders: {
      executor: "ramping-vus",
      stages: [
        { duration: warmup, target: vus },
        { duration: duration, target: vus },
        { duration: "30s", target: 0 }
      ]
    }
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<750", "p(99)<1500"]
  }
};

export default function () {
  const orderId = `order-${__VU}-${__ITER}`;
  const response = http.get(`${baseUrl}/orders/${orderId}`);
  check(response, {
    "status is 200": (r) => r.status === 200,
    "has product": (r) => Boolean(r.json("product.productId"))
  });
  sleep(1);
}
