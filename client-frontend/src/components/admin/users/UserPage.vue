<template>
  <Tabs :tabs="tabs" :vertical="true" />
</template>

<script>
import Tabs from "@/components/common/Tabs";
import NewUser from "./NewUser";
import FindUsers from "./FindUsers";
export default {
  name: "UserPage",
  components: {
    Tabs
  },
  data() {
    return {
      tabs: [
        {
          id: 0,
          name: "查找用户",
          icon: "mdi-account-search",
          component: FindUsers
        },
        {
          id: 1,
          name: "新增用户",
          icon: "mdi-account-plus",
          component: NewUser
        }
      ]
    };
  }
};
</script>

<style></style>
