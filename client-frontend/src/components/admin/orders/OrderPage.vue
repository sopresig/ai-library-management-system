<template>
  <Tabs :tabs="tabs" :vertical="true" />
</template>

<script>
import Tabs from "@/components/common/Tabs";
import FindOrders from "./FindOrders";
import FindOrdersHistory from "./FindOrdersHistory";
import FindOrdersOverdue from "./FindOrdersOverdue";
export default {
  name: "OrderPage",
  components: {
    Tabs
  },
  data() {
    return {
      tabs: [
        {
          id: 0,
          name: "查询",
          icon: "mdi-file-find",
          component: FindOrders
        },
        {
          id: 1,
          name: "历史记录",
          icon: "mdi-history",
          component: FindOrdersHistory
        },
        {
          id: 2,
          name: "逾期订单",
          icon: "mdi-alert-outline",
          component: FindOrdersOverdue
        }
      ]
    };
  }
};
</script>

<style></style>
