<template>
  <Tabs :tabs="tabs" :vertical="true" />
</template>

<script>
import Tabs from "@/components/common/Tabs";
import ChangePassword from "./ChangePassword";
import EditAccount from "./EditAccount";
export default {
  name: "AccountPages",
  components: {
    Tabs
  },
  data() {
    return {
      tabs: [
        {
          id: 0,
          name: "编辑账户",
          icon: "mdi-account",
          component: EditAccount
        },
        {
          id: 1,
          name: "修改密码",
          icon: "mdi-lock",
          component: ChangePassword
        }
      ]
    };
  }
};
</script>

<style></style>
