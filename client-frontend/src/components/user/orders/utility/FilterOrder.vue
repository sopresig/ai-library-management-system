<template>
  <v-menu bottom transition="slide-y-transition">
    <template v-slot:activator="{ on, attrs }">
      <v-btn color="indigo darken-4" dark v-bind="attrs" v-on="on" class="ma-5">
        <v-icon left dark>
          mdi-filter
        </v-icon>
        筛选
      </v-btn>
    </template>
    <v-list>
      <v-list-item link @click="filterCollectionOverdue">
        <v-list-item-title>领取逾期</v-list-item-title>
      </v-list-item>
      <v-list-item link @click="filterReturnOverdue">
        <v-list-item-title>归还逾期</v-list-item-title>
      </v-list-item>
      <v-list-item link @click="filterAll">
        <v-list-item-title>全部</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script>
export default {
  name: "FilterOrder",
  methods: {
    filterCollectionOverdue() {
      this.$emit("collectionOrders");
    },
    filterReturnOverdue() {
      this.$emit("returnOrders");
    },

    filterAll() {
      this.$emit("allOrders");
    }
  }
};
</script>

<style></style>
