#!/usr/bin/env bash
# API smoke test (through the gateway at :8080). Run right after start-all.sh: it depends on freshly seeded data.
# Covers: catalog / category filter / 404 / login (success + failure) / 401 / cart / checkout preview /
#         order placement / stock deduction / pay / cancel (stock restored) / sold-out rejection.
set -uo pipefail
GW=${GW:-http://localhost:8080}
pass=0
fail=0

c() { curl -s --noproxy '*' "$@"; }
# Request bodies go through stdin: on Windows Git Bash, Chinese in command-line args gets transcoded via the ANSI codepage
j() { local body=$1; shift; printf '%s' "$body" | curl -s --noproxy '*' --data-binary @- -H 'Content-Type: application/json' "$@"; }
check() { # check <desc> <haystack> <needle>
  if printf '%s' "$2" | grep -q -- "$3"; then pass=$((pass + 1)); echo "  ok   $1"; else fail=$((fail + 1)); echo "  FAIL $1"; echo "       expected: $3"; echo "       got: ${2:0:300}"; fi
}

echo "== catalog"
check "category list" "$(c $GW/api/categories)" '"name":"乌龙茶"'
check "product list has 6 items" "$(c $GW/api/products | grep -o '"id":"' | wc -l | tr -d ' ')" '^6$'
check "category filter green -> 2" "$(c "$GW/api/products?category=green" | grep -o '"id":"' | wc -l | tr -d ' ')" '^2$'
check "low stock status" "$(c $GW/api/products/peach-oolong)" '"stockStatus":"LOW_STOCK"'
check "product not found" "$(c $GW/api/products/not-exist)" '"code":"PRODUCT_NOT_FOUND"'

echo "== auth"
check "wrong password" "$(j '{"username":"demo","password":"x"}' -X POST $GW/api/auth/login)" '用户名或密码错误'
check "empty username" "$(j '{"username":"","password":"x"}' -X POST $GW/api/auth/login)" '请输入用户名'
check "cart requires login" "$(c $GW/api/cart)" '"code":"UNAUTHORIZED"'
LOGIN=$(j '{"username":"demo","password":"tapestry2026"}' -X POST $GW/api/auth/login)
check "login ok" "$LOGIN" '"displayName":"茶客_demo"'
TOKEN=$(printf '%s' "$LOGIN" | sed -E 's/.*"token":"([^"]+)".*/\1/')
A=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')
check "me" "$(c "${A[@]}" $GW/api/users/me)" '"username":"demo"'
check "forged X-User-Id is ignored" "$(c -H 'X-User-Id: 2' $GW/api/cart)" '"code":"UNAUTHORIZED"'

echo "== cart"
c "${A[@]}" -X DELETE $GW/api/cart >/dev/null
check "add sold-out rejected" "$(j '{"productId":"jasmine-white","qty":1}' "${A[@]}" -X POST $GW/api/cart/items)" '"code":"SOLD_OUT"'
j '{"productId":"peach-oolong","qty":1}' "${A[@]}" -X POST $GW/api/cart/items >/dev/null
CART=$(j '{"productId":"peach-oolong","qty":1}' "${A[@]}" -X POST $GW/api/cart/items)
check "merge qty -> 2" "$CART" '"qty":2'
check "shipping free when >= 99 (178)" "$CART" '"shippingFee":0.00'
CART=$(j '{"qty":1}' "${A[@]}" -X PUT $GW/api/cart/items/peach-oolong)
check "89 -> shipping 10" "$CART" '"shippingFee":10.00'
check "pay amount 99" "$CART" '"payAmount":99.00'
CART=$(j '{"qty":9}' "${A[@]}" -X PUT $GW/api/cart/items/peach-oolong)
check "qty > stock marks unavailable" "$CART" '库存不足，仅剩 8 件'
j '{"qty":2}' "${A[@]}" -X PUT $GW/api/cart/items/peach-oolong >/dev/null

echo "== checkout"
check "preview" "$(j '{"source":"CART"}' "${A[@]}" -X POST $GW/api/orders/preview)" '"payAmount":178.00'
check "phone validation" "$(j '{"source":"CART","receiverName":"张三","receiverPhone":"123","receiverAddress":"杭州市西湖区"}' "${A[@]}" -X POST $GW/api/orders)" '手机号格式不正确'
ORDER=$(j '{"source":"CART","receiverName":"张三","receiverPhone":"13800000001","receiverAddress":"杭州市西湖区文三路 1 号"}' "${A[@]}" -X POST $GW/api/orders)
check "order created" "$ORDER" '"status":"PENDING_PAYMENT"'
NO=$(printf '%s' "$ORDER" | sed -E 's/.*"orderNo":"([^"]+)".*/\1/')
check "cart cleared after order" "$(c "${A[@]}" $GW/api/cart)" '"totalCount":0'
check "stock deducted 8 -> 6" "$(c $GW/api/products/peach-oolong)" '"stock":6'
check "pay" "$(c "${A[@]}" -X POST $GW/api/orders/$NO/pay)" '"status":"PAID"'
check "cancel paid rejected" "$(c "${A[@]}" -X POST $GW/api/orders/$NO/cancel)" '"code":"ORDER_STATUS_INVALID"'

D=$(j '{"source":"DIRECT","items":[{"productId":"aged-puer","qty":1}],"receiverName":"张三","receiverPhone":"13800000001","receiverAddress":"杭州市西湖区文三路 1 号"}' "${A[@]}" -X POST $GW/api/orders)
check "direct buy" "$D" '"payAmount":388.00'
NO2=$(printf '%s' "$D" | sed -E 's/.*"orderNo":"([^"]+)".*/\1/')
check "cancel pending" "$(c "${A[@]}" -X POST $GW/api/orders/$NO2/cancel)" '"status":"CANCELLED"'
check "stock restored 23" "$(c $GW/api/products/aged-puer)" '"stock":23'
check "insufficient stock" "$(j '{"source":"DIRECT","items":[{"productId":"peach-oolong","qty":7}],"receiverName":"张三","receiverPhone":"13800000001","receiverAddress":"杭州"}' "${A[@]}" -X POST $GW/api/orders)" '「蜜桃乌龙」库存不足'
check "order list filter" "$(c "${A[@]}" "$GW/api/orders?status=CANCELLED" | grep -o '"orderNo"' | wc -l | tr -d ' ')" '^1$'
check "other user cannot see order" "$(c -H "Authorization: Bearer $(j '{"username":"demo2","password":"tapestry2026"}' -X POST $GW/api/auth/login | sed -E 's/.*"token":"([^"]+)".*/\1/')" $GW/api/orders/$NO)" '"code":"ORDER_NOT_FOUND"'

echo
echo "passed=$pass failed=$fail"
[ "$fail" = 0 ]
