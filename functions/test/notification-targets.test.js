"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {selectLatestNotificationDevice} = require("../notification-targets");

test("사용자별 마지막 로그인 기기 하나를 선택한다", () => {
  const selected = selectLatestNotificationDevice([
    {id: "phone", token: "old", lastLoginAt: "2026-08-20T10:00:00Z"},
    {id: "tablet", token: "latest", lastLoginAt: "2026-08-22T09:00:00Z"},
    {id: "spare", token: "older", lastLoginAt: "2026-08-21T12:00:00Z"},
  ]);

  assert.equal(selected.id, "tablet");
  assert.equal(selected.token, "latest");
});

test("기존 문서는 updatedAt을 마지막 로그인 시각으로 사용한다", () => {
  const selected = selectLatestNotificationDevice([
    {id: "old-app", token: "old", updatedAt: "2026-08-20T10:00:00Z"},
    {id: "new-app", token: "new", updatedAt: "2026-08-22T10:00:00Z"},
  ]);

  assert.equal(selected.id, "new-app");
});
