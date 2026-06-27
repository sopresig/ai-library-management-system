import isFinite from "lodash/isFinite";
export const rules = {
  required: v => !!v || "必填",
  number: v => (!!v && isFinite(Number(v))) || "请输入数字",
  year: v => {
    const data = Number(v);
    const currYear = new Date().getFullYear();
    return (
      (!!data && isFinite(data) && data >= 1000 && data <= currYear) ||
      "出版年份无效，有效范围：1000 - " + currYear
    );
  },
  email: v => {
    const pattern = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return pattern.test(v) || "邮箱格式无效。";
  },
  minLength(min, v, text) {
    return (
      (!!v && v.length >= min) || "至少需要 " + min + " 个" + text
    );
  }
};
