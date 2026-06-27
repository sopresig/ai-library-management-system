<template>
  <Tabs :tabs="tabs" :vertical="true" />
</template>

<script>
import Tabs from "@/components/common/Tabs";
import NewBook from "./NewBook";
import FindBooks from "./FindBooks";
export default {
  name: "BookPage",
  components: {
    Tabs
  },
  data() {
    return {
      tabs: [
        { id: 0, name: "查找图书", icon: "mdi-magnify", component: FindBooks },
        {
          id: 1,
          name: "新增图书",
          icon: "mdi-book-plus",
          component: NewBook
        }
      ]
    };
  }
};
</script>

<style></style>
